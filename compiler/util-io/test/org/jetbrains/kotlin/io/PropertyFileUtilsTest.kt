/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
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

        val tempFile = Files.createTempFile("klib-manifest-", ".properties")
        try {
            Files.write(tempFile, manifest.toByteArray(StandardCharsets.UTF_8))
            val fromPath = tempFile.readProperties()
            assertEquals(createJdkProperties(manifest), fromPath)
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
        )
        for (sample in samples) {
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
    fun `writeProperties roundtrip`() {
        val original = Properties().apply {
            setProperty("unique_name", "org.example.test")
            setProperty("abi_version", "2.5.0")
            setProperty("builtins_platform", "NATIVE")
        }
        val tempFile = Files.createTempFile("props-roundtrip-", ".properties")
        try {
            tempFile.writeProperties(original)
            val reloaded = tempFile.readProperties()
            assertEquals(original, reloaded)
        } finally {
            Files.deleteIfExists(tempFile)
        }
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
