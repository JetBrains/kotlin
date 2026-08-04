/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.jvm.KaptConfiguration
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Paths

@OptIn(ExperimentalBuildToolsApi::class)
class KaptSmokeTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of Kapt configuration application")
    @TestMetadata("kapt-project")
    fun testKaptConfiguration(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val kaptClasspath = System.getProperty("KAPT_COMPILER_PLUGIN").split(File.pathSeparator).map { Paths.get(it) }
        jvmProject(strategyConfig) {
            val module = module("kapt-project")
            module.compile(compilationConfigAction = {
                val kaptConfig = it.kaptCompilerPluginBuilder(kaptClasspath).apply {
                    this[KaptConfiguration.VERBOSE] = true
                }.withStubsPhase().withAptPhase().apply {
                    this[KaptConfiguration.AptPhase.PROCESS_INCREMENTALLY] = false
                }.build()
                it.compilerArguments[COMPILER_PLUGINS] = listOf(kaptConfig.toCompilerPlugin())
            }) {
                println(logLines.flatMap { it.value }.joinToString("\n"))
            }
        }
    }
}
