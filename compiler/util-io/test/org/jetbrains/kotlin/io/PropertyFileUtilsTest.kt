/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties

class PropertyFileUtilsTest {

    @Test
    fun `standard KLIB manifest properties`() {
        val manifest = """
            abi_version=2.5.0
            builtins_platform=NATIVE
            compiler_version=2.5.20-dev-1225
            header=true
            ir_provider=kotlin.ir.klib
            metadata_version=2.5.0
            native_targets=ios_arm64 ios_simulator_arm64 linux_x64
            short_name=sample
            unique_name=org.example.sample_klib
        """.trimIndent()

        assertEquivalentToJdkProperties(manifest)
        val parsed = createPropertiesFromBytes(manifest.toByteArray(StandardCharsets.UTF_8)) as ManifestProperties
        assertFalse(parsed.hasDottedKey)

        val tempFile = Files.createTempFile("klib-manifest-", ".properties")
        try {
            Files.write(tempFile, manifest.toByteArray(StandardCharsets.UTF_8))
            val fromPath = tempFile.readProperties() as ManifestProperties
            assertEquals(createJdkProperties(manifest), fromPath)
            assertFalse(fromPath.hasDottedKey)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun `empty whitespace and comment-only inputs`() {
        val samples = listOf(
            "",
            "   \t\u000C  ",
            "\n\r\n\r\n   \t\n",
            "# leading hash comment",
            "! leading bang comment\r\n  # indented hash comment\n\t! indented bang comment",
            "# comment ending with backslash \\\n! another comment ending with backslash \\",
        )
        for (sample in samples) {
            assertEquivalentToJdkProperties(sample)
            val parsed = createPropertiesFromBytes(sample.toByteArray(StandardCharsets.UTF_8))
            assertTrue(parsed.isEmpty)
        }
    }

    @Test
    fun `utf8 bom at start of byte array`() {
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())

        val onlyBom = createPropertiesFromBytes(bom)
        assertTrue(onlyBom.isEmpty)

        val withLeadingComment = "# comment after BOM\nunique_name=sample\nabi_version=2.5.0\n"
        val parsedAfterComment = createPropertiesFromBytes(bom + withLeadingComment.toByteArray(StandardCharsets.UTF_8))
        assertEquals(createJdkProperties(withLeadingComment), parsedAfterComment)

        val withDirectKey = "unique_name=sample\nshort_name=short\n"
        val parsedDirectKey = createPropertiesFromBytes(bom + withDirectKey.toByteArray(StandardCharsets.UTF_8))
        assertEquals(createJdkProperties(withDirectKey), parsedDirectKey)
    }

    @Test
    fun `separators equals colon whitespace and empty keys or values`() {
        val content = listOf(
            "k1=v1",
            "k2:v2",
            "k3 v3",
            "k4\tv4",
            "k5\u000Cv5",
            "k6 = v6",
            "k7 : v7",
            "k8   \t\u000C  v8",
            "k9 == v9",
            "k10 :: v10",
            "k11 = :v11",
            "k12 : =v12",
            "keyOnlyNoSep",
            "keyOnlyTrailingSpaces   \t",
            "keyEmptyEquals=",
            "keyEmptyColon:",
            "keyEmptyEqualsSpaces =   ",
            "=valueForEmptyKey",
        ).joinToString("\n")

        assertEquivalentToJdkProperties(content)
    }

    @Test
    fun `comments trailing backslashes and hash or bang inside values`() {
        val content = """
            # comment with trailing backslash \
            visible_after_comment = yes
              ! indented bang comment with trailing backslash \
            also_visible = true
            inline_markers = value # not a comment ! still value
            continued_marker = first \
              #second_not_a_comment \
              !third_not_a_comment
        """.trimIndent()

        assertEquivalentToJdkProperties(content)
    }

    @Test
    fun `escape sequences and unicode escapes in keys and values`() {
        val content = listOf(
            "tab_nl_cr_ff = a\\tb\\nc\\rd\\fe",
            "backslashes = one\\\\two\\\\\\\\three",
            "escaped\\ key\\=with\\:seps\\#and\\!bang = val\\=1\\:2\\#3\\!4",
            "unknown_escapes = \\a\\b\\g\\z",
            "unicode_upper_lower = \\u004B\\u006f\\u03B1\\u4e2d\\u6587",
            "key_\\u003d_unicode = \\u0020leading_space_preserved",
        ).joinToString("\n")

        assertEquivalentToJdkProperties(content)
    }

    @Test
    fun `malformed unicode escapes throw IllegalArgumentException`() {
        val truncatedBytes = "key=\\u12".toByteArray(StandardCharsets.UTF_8)
        assertThrows<IllegalArgumentException> {
            createPropertiesFromBytes(truncatedBytes)
        }

        val bareUnicodeEscapeBytes = "key=\\u".toByteArray(StandardCharsets.UTF_8)
        assertThrows<IllegalArgumentException> {
            createPropertiesFromBytes(bareUnicodeEscapeBytes)
        }

        val nonHexDigitBytes = "key=\\u12G4".toByteArray(StandardCharsets.UTF_8)
        assertThrows<IllegalArgumentException> {
            createPropertiesFromBytes(nonHexDigitBytes)
        }

        val malformedKeyBytes = "k\\u00Zz=value".toByteArray(StandardCharsets.UTF_8)
        assertThrows<IllegalArgumentException> {
            createPropertiesFromBytes(malformedKeyBytes)
        }
    }

    @Test
    fun `multiline backslash continuations across LF CRLF CR blank lines and EOF`() {
        val lfAndCrlf = buildString {
            append("val_cont = alpha \\\n   beta \\\r\n\t\u000Cgamma\n")
            append("key_\\\n   cont = joined_key\n")
            append("key_escaped_space\\ \\\n   cont = space_in_key\n")
            append("sep_after_cont\\\n   = value_after_cont_sep\n")
            append("space_sep_before_cont \\\n   value_after_space_cont\n")
            append("multi_empty_cont = start\\\n   \\\n   end\n")
            append("even_backslashes = keep_slash\\\\\nnext_after_even = ok\n")
            append("odd_three_backslashes = slash_and_cont\\\\\\\n   tail\n")
            append("cont_then_blank = stop_at_blank\\\n\nafter_blank = present\n")
            append("\\\n\n# comment after empty continuation\nafter_empty_cont = 42\n")
            append("trailing_eof_backslash = trimmed\\")
        }
        assertEquivalentToJdkProperties(lfAndCrlf)

        val crOnly = "cr_cont = first\\\r  second\rcr_plain = third\r"
        assertEquivalentToJdkProperties(crOnly)

        val loneBackslashWithNewline = "\\\n"
        assertEquivalentToJdkProperties(loneBackslashWithNewline)

        val loneBackslashAtEof = "\\"
        assertEquivalentToJdkProperties(loneBackslashAtEof)
    }

    @Test
    fun `non-ascii utf8 keys and values on fast path and escaped path`() {
        val content = listOf(
            "ключ = значение",
            "日本語キー = こんにちは世界",
            "emoji_key = rocket 🚀 sparkles ✨",
            "escaped_utf8_ключ\\ 1 = первой\\\n  второй\\t🚀",
        ).joinToString("\n")

        assertEquivalentToJdkProperties(content)

        val tempFile = Files.createTempFile("utf8-roundtrip-", ".properties")
        try {
            val original = createJdkProperties(content)
            tempFile.writeProperties(original)
            val reloaded = tempFile.readProperties()
            assertEquals(original, reloaded)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun `hasDottedKey tracks literal escaped and mutated dotted keys`() {
        val withoutDottedKeys = "unique_name=org.jetbrains.kotlin.sample\nabi_version=2.5.0\n"
        val propsNoDots = createPropertiesFromBytes(withoutDottedKeys.toByteArray(StandardCharsets.UTF_8)) as ManifestProperties
        assertFalse(propsNoDots.hasDottedKey)

        val withLiteralDottedKey = "unique_name=cinterop_posix\nlinkerOpts.linux_x64=-lpthread\n"
        val propsLiteralDot = createPropertiesFromBytes(withLiteralDottedKey.toByteArray(StandardCharsets.UTF_8)) as ManifestProperties
        assertTrue(propsLiteralDot.hasDottedKey)

        val withUnicodeEscapedDot = "compilerOpts\\u002earm64=-O2\n"
        val propsEscapedDot = createPropertiesFromBytes(withUnicodeEscapedDot.toByteArray(StandardCharsets.UTF_8)) as ManifestProperties
        assertTrue(propsEscapedDot.hasDottedKey)

        propsNoDots.setProperty("headers.ios", "stdbool.h")
        assertTrue(propsNoDots.hasDottedKey)
    }

    private fun createJdkProperties(content: String): Properties {
        val expected = Properties()
        expected.load(StringReader(content))
        return expected
    }

    private fun assertEquivalentToJdkProperties(content: String) {
        val expected = createJdkProperties(content)
        val actual = createPropertiesFromBytes(content.toByteArray(StandardCharsets.UTF_8))
        assertEquals(expected, actual)
        assertEquals(expected.stringPropertyNames(), actual.stringPropertyNames())
        for (name in expected.stringPropertyNames()) {
            assertEquals(expected.getProperty(name), actual.getProperty(name))
        }
    }
}
