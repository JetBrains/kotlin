/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

import junit.framework.TestCase
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.buildIDELibraryName
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.isGradleLibraryName
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.parseIDELibraryName

class KotlinNativeLibraryNameUtilTest : TestCase() {

    fun testBuildIDELibraryName() {
        assertEquals(
            "Kotlin/Native 1.3.60 - stdlib",
            buildIDELibraryName("1.3.60", "stdlib", emptyList())
        )

        assertEquals(
            "Kotlin/Native 1.3.60-eap-23 - Accelerate [macos_x64]",
            buildIDELibraryName("1.3.60-eap-23", "Accelerate", listOf("macos_x64"))
        )

        assertEquals(
            "Kotlin/Native 1.3.60-eap-23 - Accelerate [ios_arm32, ios_arm64, ios_x64]",
            buildIDELibraryName("1.3.60-eap-23", "Accelerate", listOf("ios_x64", "ios_arm32", "ios_arm64"))
        )

        assertEquals(
            "Kotlin/Native 1.3.60-eap-23 - Accelerate [ios_arm32, ios_arm64(*), ios_x64]",
            buildIDELibraryName("1.3.60-eap-23", "Accelerate", listOf("ios_x64", "ios_arm32", "ios_arm64"), "ios_arm64")
        )
    }

    fun testParseIDELibraryName() {
        assertEquals(
            Triple("1.3.60", "stdlib", null),
            parseIDELibraryName("Kotlin/Native 1.3.60 - stdlib")
        )

        assertEquals(
            Triple("1.3.60-eap-23", "Accelerate", "macos_x64"),
            parseIDELibraryName("Kotlin/Native 1.3.60-eap-23 - Accelerate [macos_x64]")
        )

        assertEquals(
            Triple("1.3.60-eap-23", "Accelerate", "ios_arm32, ios_arm64, ios_x64"),
            parseIDELibraryName("Kotlin/Native 1.3.60-eap-23 - Accelerate [ios_arm32, ios_arm64, ios_x64]")
        )

        assertEquals(
            Triple("1.3.60-eap-23", "Accelerate", "ios_arm32, ios_arm64(*), ios_x64"),
            parseIDELibraryName("Kotlin/Native 1.3.60-eap-23 - Accelerate [ios_arm32, ios_arm64(*), ios_x64]")
        )

        assertNull(parseIDELibraryName("Kotlin/Native - something unexpected"))

        assertNull(parseIDELibraryName("foo.klib"))

        assertNull(parseIDELibraryName("Gradle: some:third-party-library:1.2"))
    }

    fun testIsGradleLibraryName() {
        assertFalse(isGradleLibraryName("Kotlin/Native 1.3.60 - stdlib"))

        assertFalse(isGradleLibraryName("Kotlin/Native 1.3.60-eap-23 - Accelerate [macos_x64]"))

        assertFalse(isGradleLibraryName("foo.klib"))

        assertTrue(isGradleLibraryName("Gradle: some:third-party-library:1.2"))
    }
}
