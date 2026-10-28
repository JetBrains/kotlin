/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.cri.CompilerRefIndexConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaVersionsOnlyCompilationTest
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
    fun testCompilerRefIndexConfigurationAvailability(kotlinToolchains: KotlinToolchains) {
        val builder = kotlinToolchains.jvm.jvmCompilationOperationBuilder(emptyList(), Path("."))
        if (KotlinToolingVersion(kotlinToolchains.getCompilerVersion()) < KotlinToolingVersion(2, 5, 20, "snapshot")) {
            val exception = assertThrows<IllegalStateException> {
                builder.compilerRefIndexConfigurationBuilder(Path("cri"))
            }
            assertEquals("The compiler reference index configuration is supported from compiler version 2.5.20.", exception.message)
        } else {
            assertInstanceOf<CompilerRefIndexConfiguration.Builder>(builder.compilerRefIndexConfigurationBuilder(Path("cri")))
        }
    }
}
