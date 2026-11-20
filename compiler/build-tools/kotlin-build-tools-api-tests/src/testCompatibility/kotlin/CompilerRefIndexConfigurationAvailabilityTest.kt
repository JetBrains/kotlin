/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.cri.CompilerRefIndexConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsPlatformToolchain.Companion.js
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain.Companion.metadata
import org.jetbrains.kotlin.buildtools.api.wasm.WasmPlatformToolchain.Companion.wasm
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertInstanceOf
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.Path

@DisplayName("The availability of the compiler reference index configuration")
class CompilerRefIndexConfigurationAvailabilityTest : BaseCompilationTest() {

    @DisplayName("The configuration builder is available from Kotlin compiler version 2.5.20 and fails with a clear message before it")
    @BtaVersionsOnlyCompilationTest
    fun testCompilerRefIndexConfigurationAvailability(testToolchains: KotlinToolchains) {
        // The [v1] toolchain is the bare compat adapter; consumers load it through loadImplementation, which adds the wrappers
        val kotlinToolchains = if (testToolchains::class.simpleName == "KotlinToolchainsV1Adapter") {
            KotlinToolchains.loadImplementation(btaClassloader)
        } else {
            testToolchains
        }
        val compilerVersion = KotlinToolingVersion(kotlinToolchains.getCompilerVersion())
        for (builder in kotlinToolchains.compilationOperationBuilders(compilerVersion)) {
            if (compilerVersion < KotlinToolingVersion(2, 5, 20, "snapshot")) {
                val exception = assertThrows<IllegalStateException> {
                    builder.compilerRefIndexConfigurationBuilder(Path("cri"))
                }
                assertEquals("The compiler reference index configuration is supported from compiler version 2.5.20.", exception.message)
            } else {
                assertInstanceOf<CompilerRefIndexConfiguration.Builder>(builder.compilerRefIndexConfigurationBuilder(Path("cri")))
            }
        }
    }

    private fun KotlinToolchains.compilationOperationBuilders(
        compilerVersion: KotlinToolingVersion,
    ): List<BaseCompilationOperation.Builder> {
        // Each builder is checked as created and after a round trip through its operation
        val builders = jvm.jvmCompilationOperationBuilder(emptyList(), Path(".")).let { listOf(it, it.build().toBuilder()) }
        // Other platforms are supported since 2.4.20
        if (compilerVersion < KotlinToolingVersion(2, 4, 20, "snapshot")) return builders
        return builders + listOf(
            js.jsKlibCompilationOperationBuilder(emptyList(), Path(".")).let { listOf(it, it.build().toBuilder()) },
            js.jsLinkingOperationBuilder(Path("."), Path(".")).let { listOf(it, it.build().toBuilder()) },
            wasm.wasmKlibCompilationOperationBuilder(emptyList(), Path(".")).let { listOf(it, it.build().toBuilder()) },
            wasm.wasmLinkingOperationBuilder(Path("."), Path(".")).let { listOf(it, it.build().toBuilder()) },
            metadata.metadataKlibCompilationOperationBuilder(emptyList(), Path(".")).let { listOf(it, it.build().toBuilder()) },
        ).flatten()
    }
}
