/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments.Companion.COMPILER_PLUGINS
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.FileDependency
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.testFederation.AffectedByCompilerPlugins
import org.junit.jupiter.api.DisplayName

@AffectedByCompilerPlugins
class SerializationPluginICTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-50901: recompiling a serializable class with a @SerialInfo annotation with default arguments from a separate file does not fail codegen")
    fun testSerialInfoDefaultsDoNotBreakIc(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = module(
                "ic-scenarios/kt-50901",
                SERIALIZATION_CORE_CLASSPATH.map { FileDependency(it) },
                compilationConfigAction = {
                    it.compilerArguments[COMPILER_PLUGINS] = listOf(SERIALIZATION_PLUGIN)
                },
            )

            module.replaceFileWithVersion("Serializable.kt", "add-empty-line")

            module.compile {
                assertCompiledSources("Serializable.kt")
            }
        }
    }
}
