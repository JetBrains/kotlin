/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.cri
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

// TODO (KT-81581): add more tests with real data to deserialize
class CriToolchainSmokeTest {
    @Test
    @DisplayName("Smoke test for the CRI toolchain lookups deserialization operation")
    fun smokeTestCriLookupDeserialization() {
        val lookupData = ByteArray(1)
        val toolchain = KotlinToolchains.loadImplementation(CriToolchainSmokeTest::class.java.classLoader)
        val operation = toolchain.cri.createCriLookupDataDeserializationOperation(lookupData)
        val lookups = toolchain.createBuildSession().use { it.executeOperation(operation) }
        assertTrue(lookups.isEmpty())
    }

    @Test
    @DisplayName("Smoke test for the CRI toolchain fileIdToPath deserialization operation")
    fun smokeTestCriFileIdToPathDeserialization() {
        val fileIdToPathData = ByteArray(1)
        val toolchain = KotlinToolchains.loadImplementation(CriToolchainSmokeTest::class.java.classLoader)
        val operation = toolchain.cri.createCriFileIdToPathDataDeserializationOperation(fileIdToPathData)
        val fileIdsToPaths = toolchain.createBuildSession().use { it.executeOperation(operation) }
        assertTrue(fileIdsToPaths.isEmpty())
    }

    @Test
    @DisplayName("Smoke test for the CRI toolchain subtype deserialization operation")
    fun smokeTestCriSubtypeDeserialization() {
        val subtypeData = ByteArray(1)
        val toolchain = KotlinToolchains.loadImplementation(CriToolchainSmokeTest::class.java.classLoader)
        val operation = toolchain.cri.createCriSubtypeDataDeserializationOperation(subtypeData)
        val subtypes = toolchain.createBuildSession().use { it.executeOperation(operation) }
        assertTrue(subtypes.isEmpty())
    }
}
