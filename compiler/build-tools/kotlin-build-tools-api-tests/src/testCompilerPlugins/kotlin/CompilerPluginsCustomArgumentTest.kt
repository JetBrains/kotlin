/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPluginOption
import org.jetbrains.kotlin.buildtools.api.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertClassDeclarationsContain
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.project
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Paths

class CompilerPluginsCustomArgumentTest : BaseCompilationTest() {
    private val knownCompilerPlugins = mapOf(
        "org.jetbrains.kotlin.noarg" to "NOARG_COMPILER_PLUGIN",
        "org.jetbrains.kotlin.assignment" to "ASSIGNMENT_COMPILER_PLUGIN",
    )

    private fun getCompilerPlugin(pluginId: String, arguments: List<CompilerPluginOption>): CompilerPlugin {
        require(pluginId in knownCompilerPlugins) { "Unknown compiler plugin: $pluginId" }
        val classpathSystemPropertyName = knownCompilerPlugins.getValue(pluginId)
        val classpath = System.getProperty(classpathSystemPropertyName).split(File.pathSeparator).map { Paths.get(it) }
        return CompilerPlugin(
            pluginId = pluginId,
            classpath = classpath,
            rawArguments = arguments,
            orderingRequirements = emptySet(),
        )
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Smoke test of compiler plugins application")
    @TestMetadata("compiler-plugins")
    fun smokeTestCompilerPluginsApplication(strategyConfig: CompilerExecutionStrategyConfiguration) {
        project(strategyConfig) {
            val module = module("compiler-plugins")
            module.compile(compilationConfigAction = {
                val noArg = getCompilerPlugin(
                    pluginId = "org.jetbrains.kotlin.noarg",
                    arguments = listOf(CompilerPluginOption("annotation", "GenerateNoArgsConstructor"))
                )
                val assignment = getCompilerPlugin(
                    pluginId = "org.jetbrains.kotlin.assignment",
                    arguments = listOf(CompilerPluginOption("annotation", "GenerateAssignment"))
                )
                it.compilerArguments[COMPILER_PLUGINS] = listOf(noArg, assignment)
            }) {
                assertOutputs(
                    "GenerateAssignment.class",
                    "GenerateNoArgsConstructor.class",
                    "AssignableClass.class",
                    "AssignableClassKt.class",
                    "SomeClass.class",
                    "AssignmentUsageKt.class", // ensures the usage of assignment was compiled
                )
                assertClassDeclarationsContain(
                    classFqn = "SomeClass",
                    setOf(
                        "public SomeClass();",
                        "public SomeClass(int, AssignableClass);",
                    )
                )
            }
        }
    }
}
