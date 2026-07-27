/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assumptions.assumeTrue

class ApplyArgumentStringsOverridingTest : BaseCompilationTest() {

    @BtaV2StrategyAgnosticCompilationTest
    fun `test applyArgumentStrings does not override existing values in new versions`(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kotlinToolchains = strategyConfig.first
        val compilerVersion = KotlinToolingVersion(kotlinToolchains.getCompilerVersion())
        val apiVersion = try {
            KotlinToolingVersion(KotlinToolchains.getVersion())
        } catch (_: NoSuchMethodError) {
            KotlinToolingVersion("2.4.10") // getVersion() was added in 2.4.20
        }
        val isNewBehaviorExpected = compilerVersion >= KotlinToolingVersion("2.5.0-snapshot") &&
                apiVersion >= KotlinToolingVersion("2.5.0-snapshot")

        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(
                compilationConfigAction = {
                    it.compilerArguments[JvmCompilerArguments.JVM_TARGET] = JvmTarget.JVM_17
                    it.compilerArguments[JvmCompilerArguments.NO_STDLIB] = true

                    it.compilerArguments.applyArgumentStrings(listOf("-no-reflect"))

                    if (isNewBehaviorExpected) {
                        assertEquals(JvmTarget.JVM_17, it.compilerArguments[JvmCompilerArguments.JVM_TARGET])
                        assertEquals(true, it.compilerArguments[JvmCompilerArguments.NO_STDLIB])
                    } else {
                        // Old versions override everything
                        assertNotEquals(JvmTarget.JVM_17, it.compilerArguments[JvmCompilerArguments.JVM_TARGET])
                        assertEquals(false, it.compilerArguments[JvmCompilerArguments.NO_STDLIB])
                    }
                    assertEquals(true, it.compilerArguments[JvmCompilerArguments.NO_REFLECT])
                },
                assertions = { assertOutputs("FooKt.class", "Bar.class", "BazKt.class") })
        }
    }
}
