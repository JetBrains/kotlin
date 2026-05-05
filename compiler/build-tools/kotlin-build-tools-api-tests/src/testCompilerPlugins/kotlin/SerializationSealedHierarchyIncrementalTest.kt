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
import org.junit.jupiter.api.DisplayName

@DisplayName("KT-86121: Incremental compilation with the serialization plugin")
class SerializationSealedHierarchyIncrementalTest : BaseCompilationTest() {

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Modifying a concrete subclass in a multi-file sealed serializable hierarchy succeeds incrementally")
    fun testIncrementalCompilationOfSealedSerializableHierarchy(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = module(
                "serialization-sealed-hierarchy",
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
}
