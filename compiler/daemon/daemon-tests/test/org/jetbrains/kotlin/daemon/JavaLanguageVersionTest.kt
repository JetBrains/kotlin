/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.JavaLanguageVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class JavaLanguageVersionTest {

    @Test
    @DisplayName("null produces unknown language version")
    fun parseNull() {
        assertUnknown(JavaLanguageVersion.parse(null))
    }

    @Test
    @DisplayName("empty string produces unknown language version")
    fun parseEmpty() {
        assertUnknown(JavaLanguageVersion.parse(""))
    }

    @Test
    @DisplayName("blank string produces unknown language version after trimming")
    fun parseBlank() {
        assertUnknown(JavaLanguageVersion.parse("   "))
    }

    @Test
    @DisplayName("non-numeric string produces unknown language version")
    fun parseNonNumeric() {
        assertUnknown(JavaLanguageVersion.parse("abc"))
    }

    @Test
    @DisplayName("string starting with '-' produces unknown language version")
    fun parseNegativeSign() {
        assertUnknown(JavaLanguageVersion.parse("-17"))
    }

    @Test
    @DisplayName("'1.8' parses to language version 8")
    fun parseLegacyShort() {
        assertEquals(8, JavaLanguageVersion.parse("1.8").version)
    }

    @Test
    @DisplayName("'1.8.0' parses to language version 8 — java.version patch suffix is also accepted")
    fun parseLegacyWithPatch() {
        assertEquals(8, JavaLanguageVersion.parse("1.8.0").version)
    }

    @Test
    @DisplayName("'1.8.0_292' parses to language version 8 — java.version update suffix is also accepted")
    fun parseLegacyFull() {
        assertEquals(8, JavaLanguageVersion.parse("1.8.0_292").version)
    }

    @Test
    @DisplayName("'1.6' parses to language version 6")
    fun parseLegacyJava6() {
        assertEquals(6, JavaLanguageVersion.parse("1.6").version)
    }

    @Test
    @DisplayName("'1.' with no minor version produces unknown language version")
    fun parseLegacyMissingMinor() {
        assertUnknown(JavaLanguageVersion.parse("1."))
    }

    @Test
    @DisplayName("'1.a' with non-numeric minor produces unknown language version")
    fun parseLegacyNonNumericMinor() {
        assertUnknown(JavaLanguageVersion.parse("1.a"))
    }

    @Test
    @DisplayName("legacy string with surrounding whitespace is trimmed before parsing")
    fun parseLegacyWhitespaceTrimmed() {
        assertEquals(8, JavaLanguageVersion.parse("  1.8  ").version)
    }

    @Test
    @DisplayName("'11' parses to language version 11")
    fun parseModernJava11() {
        assertEquals(11, JavaLanguageVersion.parse("11").version)
    }

    @Test
    @DisplayName("'17' parses to language version 17")
    fun parseModernJava17() {
        assertEquals(17, JavaLanguageVersion.parse("17").version)
    }

    @Test
    @DisplayName("'21' parses to language version 21")
    fun parseModernJava21() {
        assertEquals(21, JavaLanguageVersion.parse("21").version)
    }

    @Test
    @DisplayName("'17.0.1' parses to language version 17 — java.version minor/patch suffix is also accepted")
    fun parseModernWithMinorAndPatch() {
        assertEquals(17, JavaLanguageVersion.parse("17.0.1").version)
    }

    @Test
    @DisplayName("'17.0.1+12-LTS' parses to language version 17 — java.version build qualifier is also accepted")
    fun parseModernWithBuildQualifier() {
        assertEquals(17, JavaLanguageVersion.parse("17.0.1+12-LTS").version)
    }

    @Test
    @DisplayName("modern string with surrounding whitespace is trimmed before parsing")
    fun parseModernWhitespaceTrimmed() {
        assertEquals(21, JavaLanguageVersion.parse("  21  ").version)
    }

    @Test
    @DisplayName("language version 8 compares less than language version 11")
    fun compareVersionLess() {
        val java8 = JavaLanguageVersion.parse("1.8")
        val java11 = JavaLanguageVersion.parse("11")
        assertTrue(java8 < java11)
    }

    @Test
    @DisplayName("language version 21 compares greater than language version 17")
    fun compareVersionGreater() {
        val java21 = JavaLanguageVersion.parse("21")
        val java17 = JavaLanguageVersion.parse("17")
        assertTrue(java21 > java17)
    }

    @Test
    @DisplayName("equal major language versions compare as equal regardless of minor/patch")
    fun compareVersionEqual() {
        val java17a = JavaLanguageVersion.parse("17")
        val java17b = JavaLanguageVersion.parse("17.0.1")
        assertEquals(0, java17a.compareTo(java17b))
    }

    @Test
    @DisplayName("'1.5' parses to language version 5")
    fun parseLegacyJava5() {
        assertEquals(5, JavaLanguageVersion.parse("1.5").version)
    }

    @Test
    @DisplayName("'1.7' parses to language version 7")
    fun parseLegacyJava7() {
        assertEquals(7, JavaLanguageVersion.parse("1.7").version)
    }

    @Test
    @DisplayName("'10' parses to language version 10 — first version to drop the legacy 1.x prefix")
    fun parseModernJava10() {
        assertEquals(10, JavaLanguageVersion.parse("10").version)
    }

    @Test
    @DisplayName("'11.0.2-openj9' parses to language version 11 — hyphen-separated qualifier is ignored")
    fun parseModernWithHyphenQualifier() {
        assertEquals(11, JavaLanguageVersion.parse("11.0.2-openj9").version)
    }

    @Test
    @DisplayName("a known language version compares greater than unknown language version, since unknown.version == -1")
    fun knownVersionComparesToUnknownAsGreater() {
        val java8 = JavaLanguageVersion.parse("1.8")
        val unknown = JavaLanguageVersion.parse(null)
        assertTrue(java8 > unknown)
    }

    @Test
    @DisplayName("JavaLanguageVersion is serializable for RMI transport")
    fun javaLanguageVersionIsSerializable() {
        val original = JavaLanguageVersion.parse("17")
        val baos = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(baos).use { it.writeObject(original) }
        val restored = java.io.ObjectInputStream(java.io.ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as JavaLanguageVersion
        }
        assertEquals(original.version, restored.version)
        assertTrue(restored >= JavaLanguageVersion.parse("11"))
        assertFalse(restored >= JavaLanguageVersion.parse("21"))
    }

    @Test
    @DisplayName("unknown JavaLanguageVersion is serializable for RMI transport")
    fun unknownJavaLanguageVersionIsSerializable() {
        val original = JavaLanguageVersion.parse(null)
        val baos = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(baos).use { it.writeObject(original) }
        val restored = java.io.ObjectInputStream(java.io.ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as JavaLanguageVersion
        }
        assertEquals(-1, restored.version)
        assertTrue(restored < JavaLanguageVersion.of(8))
    }

    @Test
    @DisplayName("of(17) returns a known language version with version == 17")
    fun ofKnownVersion() {
        assertEquals(17, JavaLanguageVersion.of(17).version)
    }

    @Test
    @DisplayName("of(8) returns the same language version as parse(\"1.8\")")
    fun ofEqualsParseForLegacyVersion() {
        assertEquals(JavaLanguageVersion.parse("1.8").version, JavaLanguageVersion.of(8).version)
    }

    @Test
    @DisplayName("of(17) and parse(\"17\") are structurally equal")
    fun ofEqualsParseStructurally() {
        assertEquals(JavaLanguageVersion.of(17), JavaLanguageVersion.parse("17"))
    }

    @Test
    @DisplayName("of(0) throws IllegalArgumentException")
    fun ofZeroThrows() {
        assertThrows(IllegalArgumentException::class.java) { JavaLanguageVersion.of(0) }
    }

    @Test
    @DisplayName("of(-1) throws IllegalArgumentException, preventing confusion with unknown language version")
    fun ofNegativeThrows() {
        assertThrows(IllegalArgumentException::class.java) { JavaLanguageVersion.of(-1) }
    }

    @Test
    @DisplayName("of(17) is serializable for RMI transport")
    fun ofIsSerializable() {
        val original = JavaLanguageVersion.of(17)
        val baos = java.io.ByteArrayOutputStream()
        java.io.ObjectOutputStream(baos).use { it.writeObject(original) }
        val restored = java.io.ObjectInputStream(java.io.ByteArrayInputStream(baos.toByteArray())).use {
            it.readObject() as JavaLanguageVersion
        }
        assertEquals(original, restored)
    }

    private fun assertUnknown(version: JavaLanguageVersion) {
        assertEquals(-1, version.version, "Expected unknown language version to have version == -1")
        assertTrue(version < JavaLanguageVersion.of(8), "Unknown language version must compare less than any known language version")
    }
}
