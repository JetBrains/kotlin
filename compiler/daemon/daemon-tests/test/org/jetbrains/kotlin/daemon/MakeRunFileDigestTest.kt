/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon

import org.jetbrains.kotlin.daemon.common.JavaLanguageVersion
import org.jetbrains.kotlin.daemon.common.makeRunFileDigest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class MakeRunFileDigestTest {

    private val classpath
        get() = listOf("/a/kotlin-compiler.jar").map { Path(it) }

    @Test
    @DisplayName("same inputs produce the same digest")
    fun sameInputsProduceSameDigest() {
        assertEquals(
            makeRunFileDigest(JavaLanguageVersion.of(17), classpath),
            makeRunFileDigest(JavaLanguageVersion.of(17), classpath)
        )
    }

    @Test
    @DisplayName("different JVM versions produce different digests")
    fun differentJvmVersionsProduceDifferentDigests() {
        assertNotEquals(
            makeRunFileDigest(JavaLanguageVersion.of(17), classpath),
            makeRunFileDigest(JavaLanguageVersion.of(21), classpath)
        )
    }

    @Test
    @DisplayName("different classpaths produce different digests")
    fun differentClasspathsProduceDifferentDigests() {
        val other = listOf("/b/kotlin-compiler.jar").map { Path(it) }
        assertNotEquals(
            makeRunFileDigest(JavaLanguageVersion.of(17), classpath),
            makeRunFileDigest(JavaLanguageVersion.of(17), other)
        )
    }

    @Test
    @DisplayName("classpath is normalised: duplicates and ordering do not affect the digest")
    fun classpathIsNormalisedBeforeDigesting() {
        val withDuplicatesAndDifferentOrder = listOf(
            "/a/kotlin-compiler.jar",
            "/a/kotlin-compiler.jar",
            "/b/scripting.jar",
        ).map { Path(it) }
        val canonical = listOf("/b/scripting.jar", "/a/kotlin-compiler.jar").map { Path(it) }
        assertEquals(
            makeRunFileDigest(JavaLanguageVersion.of(17), canonical),
            makeRunFileDigest(JavaLanguageVersion.of(17), withDuplicatesAndDifferentOrder)
        )
    }
}
