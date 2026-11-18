/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.UnsupportedToolchainException
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.konan.NativePlatformToolchain
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

@DisplayName("Requesting a toolchain that the loaded implementation does not provide")
class UnsupportedToolchainTest : BaseCompilationTest() {

    private interface UnknownToolchain : KotlinToolchains.Toolchain

    @DisplayName("Platform toolchains are available from the compiler version that introduced them and fail with UnsupportedToolchainException before it")
    @BtaVersionsOnlyCompilationTest
    fun testPlatformToolchainAvailability(testToolchains: KotlinToolchains) {
        val kotlinToolchains = testToolchains.withCompatibilityWrappers()
        val compilerVersion = KotlinToolingVersion(kotlinToolchains.getCompilerVersion())
        // JVM is available on every version: the compat adapter provides it before 2.3.0
        assertNotNull(kotlinToolchains.getToolchain(JvmPlatformToolchain::class.java))
        val toolchainsSince = mapOf(
            JsPlatformToolchain::class.java to KotlinToolingVersion(2, 4, 20, "snapshot"),
            WasmPlatformToolchain::class.java to KotlinToolingVersion(2, 4, 20, "snapshot"),
            KotlinMetadataPlatformToolchain::class.java to KotlinToolingVersion(2, 4, 20, "snapshot"),
            NativePlatformToolchain::class.java to KotlinToolingVersion(2, 5, 20, "snapshot"),
        )
        for ([type, since] in toolchainsSince) {
            if (compilerVersion >= since) {
                assertTrue(type.isInstance(kotlinToolchains.getToolchain(type)), "Expected an instance of ${type.name}")
            } else {
                kotlinToolchains.assertUnsupported(type)
            }
        }
    }

    @DisplayName("An unknown toolchain type fails with UnsupportedToolchainException")
    @BtaVersionsOnlyCompilationTest
    fun testUnknownToolchain(testToolchains: KotlinToolchains) {
        testToolchains.withCompatibilityWrappers().assertUnsupported(UnknownToolchain::class.java)
    }

    // The [v1] toolchain is the bare compat adapter; consumers load it through loadImplementation, which adds the wrappers
    private fun KotlinToolchains.withCompatibilityWrappers(): KotlinToolchains =
        if (this::class.simpleName == "KotlinToolchainsV1Adapter") KotlinToolchains.loadImplementation(btaClassloader) else this

    private fun KotlinToolchains.assertUnsupported(type: Class<out KotlinToolchains.Toolchain>) {
        val exception = assertThrows<UnsupportedToolchainException> { getToolchain(type) }
        assertEquals(type, exception.toolchainType)
        assertEquals(getCompilerVersion(), exception.compilerVersion)
    }
}
