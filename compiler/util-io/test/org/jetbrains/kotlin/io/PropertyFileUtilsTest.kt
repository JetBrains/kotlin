/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Properties
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

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

    @Test
    fun `60 KB depends property remains unmaterialized on unrelated lookups`() {
        val largeDepends = createLargeDependsValue("dep_module_")
        assertTrue(largeDepends.length >= 60_000)
        val manifest = createManifestWithDepends(largeDepends)

        val props = createPropertiesFromBytes(manifest.toByteArray(StandardCharsets.UTF_8)) as ManifestProperties
        assertTrue(props.hasDeferredDepends)

        assertEquals("org.example.app", props.getProperty("unique_name"))
        assertEquals("2.5.0", props.getProperty("abi_version", "fallback"))
        assertEquals("fallback", props.getProperty("non_existent", "fallback"))
        assertEquals("NATIVE", props["builtins_platform"])
        assertEquals("default_val", props.getOrDefault("non_existent", "default_val"))
        assertTrue(props.containsKey("native_targets"))
        assertFalse(props.containsKey("non_existent"))
        assertNull(props.getProperty("non_existent"))

        assertTrue(props.hasDeferredDepends)
    }

    @Test
    fun `60 KB depends property materializes accurately across all accessors and mutators`() {
        val largeDepends = createLargeDependsValue("transitive_klib_")
        val manifest = createManifestWithDepends(largeDepends)
        val manifestBytes = manifest.toByteArray(StandardCharsets.UTF_8)
        val expected = createJdkProperties(manifest)

        val viaGetProperty = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertTrue(viaGetProperty.hasDeferredDepends)
        assertEquals(largeDepends, viaGetProperty.getProperty("depends"))
        assertFalse(viaGetProperty.hasDeferredDepends)

        val viaGet = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(largeDepends, viaGet["depends"])
        assertFalse(viaGet.hasDeferredDepends)

        val viaContainsKey = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertTrue(viaContainsKey.containsKey("depends"))
        assertFalse(viaContainsKey.hasDeferredDepends)

        val viaStringPropertyNames = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(expected.stringPropertyNames(), viaStringPropertyNames.stringPropertyNames())
        assertFalse(viaStringPropertyNames.hasDeferredDepends)

        val viaPropertyNames = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        val collectedNames = mutableSetOf<String>()
        val enumeration = viaPropertyNames.propertyNames()
        while (enumeration.hasMoreElements()) {
            collectedNames.add(enumeration.nextElement() as String)
        }
        assertEquals(expected.stringPropertyNames(), collectedNames)
        assertFalse(viaPropertyNames.hasDeferredDepends)

        val viaEntries = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(expected.entries, viaEntries.entries)
        assertFalse(viaEntries.hasDeferredDepends)

        val viaKeysAndValues = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(expected.keys, viaKeysAndValues.keys)
        assertTrue(viaKeysAndValues.values.contains(largeDepends))
        assertFalse(viaKeysAndValues.hasDeferredDepends)

        val viaSize = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(expected.size, viaSize.size)
        assertFalse(viaSize.isEmpty)
        assertFalse(viaSize.hasDeferredDepends)

        val viaRemove = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(largeDepends, viaRemove.remove("depends"))
        assertFalse(viaRemove.hasDeferredDepends)
        assertNull(viaRemove.getProperty("depends"))

        val viaSetProperty = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(largeDepends, viaSetProperty.setProperty("depends", "stdlib"))
        assertFalse(viaSetProperty.hasDeferredDepends)
        assertEquals("stdlib", viaSetProperty.getProperty("depends"))

        val viaStore = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        val writer = StringWriter()
        viaStore.store(writer, null)
        assertFalse(viaStore.hasDeferredDepends)
        assertEquals(expected, createJdkProperties(writer.toString()))

        val unmaterializedA = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        val unmaterializedB = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertEquals(unmaterializedA, unmaterializedB)
        assertEquals(expected.hashCode(), unmaterializedA.hashCode())
        assertFalse(unmaterializedA.hasDeferredDepends)
        assertFalse(unmaterializedB.hasDeferredDepends)

        val unmaterializedForClone = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        val cloned = unmaterializedForClone.clone() as Properties
        assertFalse(unmaterializedForClone.hasDeferredDepends)
        assertEquals(expected, cloned)

        val viaClear = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        viaClear.clear()
        assertFalse(viaClear.hasDeferredDepends)
        assertTrue(viaClear.isEmpty)
    }

    @Test
    fun `duplicate depends definitions and concurrent lazy materialization`() {
        val escapedThenFast = "depends=stdlib foo\\:bar\ndepends=stdlib plain_dep\nunique_name=sample\n"
        assertEquivalentToJdkProperties(escapedThenFast)

        val fastThenEscaped = "depends=stdlib plain_dep\ndepends=stdlib foo\\:bar\nunique_name=sample\n"
        assertEquivalentToJdkProperties(fastThenEscaped)

        val largeDepends = createLargeDependsValue("concurrent_dep_")
        val manifestBytes = createManifestWithDepends(largeDepends).toByteArray(StandardCharsets.UTF_8)
        val sharedProps = createPropertiesFromBytes(manifestBytes) as ManifestProperties
        assertTrue(sharedProps.hasDeferredDepends)

        val threadCount = 8
        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(threadCount)
        try {
            val futures = buildList {
                for (workerIdx in 0 until threadCount) {
                    add(
                        executor.submit(
                            Callable {
                                startLatch.await()
                                if (workerIdx % 2 == 0) {
                                    sharedProps.getProperty("depends")
                                } else {
                                    sharedProps.stringPropertyNames().contains("depends").toString()
                                }
                            }
                        )
                    )
                }
            }
            startLatch.countDown()
            for (workerIdx in 0 until futures.size) {
                val value = futures[workerIdx].get()
                if (workerIdx % 2 == 0) {
                    assertEquals(largeDepends, value)
                } else {
                    assertEquals("true", value)
                }
            }
            assertFalse(sharedProps.hasDeferredDepends)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun createLargeDependsValue(prefix: String): String = buildString(72_000) {
        append("stdlib")
        for (idx in 0 until 1800) {
            append(' ')
            append(prefix)
            append("package_segment_number_")
            append(idx)
        }
    }

    private fun createManifestWithDepends(dependsValue: String): String =
        "abi_version=2.5.0\n" +
            "builtins_platform=NATIVE\n" +
            "depends=$dependsValue\n" +
            "native_targets=ios_arm64 linux_x64\n" +
            "unique_name=org.example.app\n"

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
