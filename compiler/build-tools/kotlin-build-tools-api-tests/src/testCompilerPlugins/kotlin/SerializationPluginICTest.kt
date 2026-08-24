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

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-86121: Modifying a concrete subclass in a multi-file sealed serializable hierarchy succeeds incrementally")
    fun testIncrementalCompilationOfSealedSerializableHierarchy(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = module(
                "ic-scenarios/serialization-sealed-hierarchy",
                SERIALIZATION_CORE_CLASSPATH.map { FileDependency(it) },
                compilationConfigAction = {
                    it.compilerArguments[COMPILER_PLUGINS] = listOf(SERIALIZATION_PLUGIN)
                },
            )

            module.replaceFileWithVersion("Bar.kt", "change")

            // before KT-86121 fix, this crashed with IndexOutOfBoundsException in
            // usesDefaultArguments() during SyntheticAccessorLowering
            module.compile {
                assertCompiledSources("Bar.kt")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-88801: Modifying a subclass of a serializable abstract class with a private field succeeds incrementally")
    fun testIncrementalCompilationOfSerializableAbstractClassWithPrivateField(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = module(
                "ic-scenarios/serialization-abstract-private-field",
                SERIALIZATION_CORE_CLASSPATH.map { FileDependency(it) },
                compilationConfigAction = {
                    it.compilerArguments[COMPILER_PLUGINS] = listOf(SERIALIZATION_PLUGIN)
                },
            )

            module.replaceFileWithVersion("SubClass.kt", "change")

            // before KT-88801 fix, this crashed with IndexOutOfBoundsException in
            // usesDefaultArguments() during SyntheticAccessorLowering
            module.compile {
                assertCompiledSources("SubClass.kt")
            }
        }
    }
}
