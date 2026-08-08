/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BuildOperation
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectCreator
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName

class ClassLoadersCachingTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Test that plugins loader is used to cache compiler plugins")
    fun testPluginsLoaderCache(project: ProjectCreator) {
        project {
            assumeTrue(this.defaultStrategyConfig is ExecutionPolicy.InProcess)

            val module = module("sandbox-plugin", moduleCompilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
            })

            module.compile(forceOutput = LogLevel.INFO) {
                assertLogContainsPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
            module.compile {
                assertLogDoesNotContainPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticCompilationTest
    @DisplayName("Test that plugins loader is not used to cache compiler plugins when disabled")
    fun testPluginsLoaderCacheDisabled(project: ProjectCreator) {
        project {
            assumeTrue(this.defaultStrategyConfig is ExecutionPolicy.InProcess)

            val module = module("sandbox-plugin", moduleCompilationConfigAction = { operation: BaseCompilationOperation.Builder ->
                operation.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(PLUGIN_SANDBOX_PLUGIN)
                operation[BuildOperation.ENABLE_CLASSLOADER_CACHE] = false
            })

            module.compile(forceOutput = LogLevel.INFO) {
                assertLogDoesNotContainPatterns(LogLevel.INFO, "Creating new classloader for classpath.*".toRegex())
            }
        }
    }
}
