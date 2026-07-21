/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectedLogLevelForRebuildReason
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class PackageInfoChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes to package-info lead to full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoChangeCausesRecompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification").modifyPackageInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes to package-info lead to full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoChangeCausesRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(moduleName = "ic-scenarios/package-info-modification").modifyPackageInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    private fun ScenarioModule.modifyPackageInfoAndAssertFullRecompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        replaceFileWithVersion("package-info.java", "add-annotation")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            // If we ever decide to handle package-info.java changes in a more granular way, we can change the assertion to:
            // assertCompiledSources("a.kt")
            // because b.kt is located in a different package
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding an annotation to a dependency's package-info.java recompiles the consumer non-incrementally (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-modification")
    fun testDependencyPackageInfoChangeCausesConsumerRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-modification/module-a",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-modification/module-b",
                dependencies = listOf(dependency),
            )
            dependency.addAnnotationAndAssertConsumerRecompiles(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding an annotation to a dependency's package-info.java recompiles the consumer non-incrementally (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-modification")
    fun testDependencyPackageInfoChangeCausesConsumerRecompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-modification/module-a",
                compileJavaSources = true,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-package-info-modification/module-b",
                dependencies = listOf(dependency),
            )
            dependency.addAnnotationAndAssertConsumerRecompiles(consumer)
        }
    }

    private fun ScenarioModule.addAnnotationAndAssertConsumerRecompiles(consumer: ScenarioModule) {
        replaceFileWithVersion("package-info.java", "add-annotation")
        compile()
        consumer.compile {
            assertCompiledSources("bpkg/UseA.kt")
        }
    }
}
