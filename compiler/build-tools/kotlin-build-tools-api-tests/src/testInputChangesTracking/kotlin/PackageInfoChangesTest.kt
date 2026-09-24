/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectedLogLevelForRebuildReason
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Disabled
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

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing package-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoRemovalCausesRecompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification").removePackageInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing package-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoRemovalCausesRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(moduleName = "ic-scenarios/package-info-modification").removePackageInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding package-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoAdditionCausesRecompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification").addPackageInfoAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding package-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoAdditionCausesRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(moduleName = "ic-scenarios/package-info-modification").addPackageInfoAndAssertFullRecompilation(strategyConfig)
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

    private fun ScenarioModule.removePackageInfoAndAssertFullRecompilation(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        deleteFile("package-info.java")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    private fun ScenarioModule.addPackageInfoAndAssertFullRecompilation(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        deleteFile("package-info.java")
        compile()
        createPredefinedFile("package-info.java", "add-annotation")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A documentation-only change of package-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoCommentOnlyChangeCausesRecompilationInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification")
                .changeOnlyPackageInfoCommentAndAssertFullRecompilation(strategyConfig)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A documentation-only change of package-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoCommentOnlyChangeCausesRecompilationExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            module(moduleName = "ic-scenarios/package-info-modification")
                .changeOnlyPackageInfoCommentAndAssertFullRecompilation(strategyConfig)
        }
    }

    private fun ScenarioModule.changeOnlyPackageInfoCommentAndAssertFullRecompilation(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        replaceFileWithVersion("package-info.java", "comment-only")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(strategyConfig),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding an annotation to a dependency's package-info.java recompiles only the affected consumer source (externally tracked)")
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
    @DisplayName("Adding an annotation to a dependency's package-info.java recompiles only the affected consumer source (internally tracked)")
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

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing one dependency package-info recompiles only consumers of that package (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-multi-package")
    fun testDependencyPackageInfoChangeIsIsolatedToAffectedPackageExternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-multi-package/module-a",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-multi-package/module-b",
                dependencies = listOf(dependency),
            )
            dependency.changeOnePackageInfoAndAssertOnlyAffectedConsumerRecompiles(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing one dependency package-info recompiles only consumers of that package (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-multi-package")
    fun testDependencyPackageInfoChangeIsIsolatedToAffectedPackageInternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-multi-package/module-a",
                compileJavaSources = true,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-package-info-multi-package/module-b",
                dependencies = listOf(dependency),
            )
            dependency.changeOnePackageInfoAndAssertOnlyAffectedConsumerRecompiles(consumer)
        }
    }

    private fun ScenarioModule.changeOnePackageInfoAndAssertOnlyAffectedConsumerRecompiles(consumer: ScenarioModule) {
        replaceFileWithVersion("apkg/package-info.java", "add-annotation")
        compile()
        consumer.compile {
            assertCompiledSources("bpkg/UseA.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing dependency package-info nullability recompiles the consumer and reports an error (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesConsumerExternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-direct-call",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.addNonNullApiAndAssertConsumerFails(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing dependency package-info nullability recompiles the consumer and reports an error (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesConsumerInternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-direct-call",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.addNonNullApiAndAssertConsumerFails(consumer)
        }
    }

    private fun ScenarioModule.addNonNullApiAndAssertConsumerFails(consumer: ScenarioModule) {
        replaceFileWithVersion("package-info.java", "add-nonnull-api")
        compile()
        consumer.compile {
            expectFailWithError(".*UseA\\.kt:\\d+:\\d+ Null cannot be a value of a non-null type 'String'.*".toRegex())
            assertCompiledSources("bpkg/UseA.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @Disabled("Follow-up of KT-62261: a fully qualified reference does not make the consumer file dirty")
    @DisplayName("Changing dependency package-info nullability recompiles a consumer using fully qualified names (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesFullyQualifiedConsumerExternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-fully-qualified",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.addNonNullApiAndAssertFullyQualifiedConsumerFails(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @Disabled("Follow-up of KT-62261: a fully qualified reference does not make the consumer file dirty")
    @DisplayName("Changing dependency package-info nullability recompiles a consumer using fully qualified names (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesFullyQualifiedConsumerInternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-fully-qualified",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.addNonNullApiAndAssertFullyQualifiedConsumerFails(consumer)
        }
    }

    private fun ScenarioModule.addNonNullApiAndAssertFullyQualifiedConsumerFails(consumer: ScenarioModule) {
        replaceFileWithVersion("package-info.java", "add-nonnull-api")
        compile()
        consumer.compile {
            expectFailWithError(".*UseAFullyQualified\\.kt:\\d+:\\d+ Null cannot be a value of a non-null type 'String'.*".toRegex())
            assertCompiledSources("bpkg/UseAFullyQualified.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing dependency package-info nullability recompiles a consumer that reaches the annotated type through its own factory (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesLocalFactoryConsumerExternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-local-factory",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.addNonNullApiAndAssertLocalFactoryConsumerFails(consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing dependency package-info nullability recompiles a consumer that reaches the annotated type through its own factory (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesLocalFactoryConsumerInternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-local-factory",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.addNonNullApiAndAssertLocalFactoryConsumerFails(consumer)
        }
    }

    private fun ScenarioModule.addNonNullApiAndAssertLocalFactoryConsumerFails(consumer: ScenarioModule) {
        replaceFileWithVersion("package-info.java", "add-nonnull-api")
        compile()
        consumer.compile {
            expectFailWithError(".*UseKotlinFactory\\.kt:\\d+:\\d+ Null cannot be a value of a non-null type 'String'.*".toRegex())
            // `bpkg/Other.kt` must stay untouched: the assertion has to distinguish a precise dirty set from a full module rebuild.
            assertCompiledSources("bpkg/KotlinFactory.kt", "bpkg/UseKotlinFactory.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Only the package-info of the first classpath entry declaring the package is tracked (externally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-first-wins")
    fun testOnlyFirstDependencyPackageInfoIsTrackedExternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val first = module(
                moduleName = "ic-scenarios/dependency-package-info-first-wins/module-first",
                compileJavaSources = true,
            )
            val second = module(
                moduleName = "ic-scenarios/dependency-package-info-first-wins/module-second",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-first-wins/module-consumer",
                dependencies = listOf(first, second),
            )
            assertOnlyFirstPackageInfoIsTracked(first, second, consumer)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Only the package-info of the first classpath entry declaring the package is tracked (internally tracked)")
    @TestMetadata("ic-scenarios/dependency-package-info-first-wins")
    fun testOnlyFirstDependencyPackageInfoIsTrackedInternallyTracked(
        strategyConfig: CompilerExecutionStrategyConfiguration,
    ) {
        jvmScenario(strategyConfig) {
            val first = module(
                moduleName = "ic-scenarios/dependency-package-info-first-wins/module-first",
                compileJavaSources = true,
            )
            val second = module(
                moduleName = "ic-scenarios/dependency-package-info-first-wins/module-second",
                compileJavaSources = true,
            )
            val consumer = trackedModule(
                moduleName = "ic-scenarios/dependency-package-info-first-wins/module-consumer",
                dependencies = listOf(first, second),
            )
            assertOnlyFirstPackageInfoIsTracked(first, second, consumer)
        }
    }

    private fun assertOnlyFirstPackageInfoIsTracked(first: ScenarioModule, second: ScenarioModule, consumer: ScenarioModule) {
        second.replaceFileWithVersion("apkg/package-info.java", "change-annotation")
        second.compile()
        consumer.compile {
            assertNoCompiledSources()
        }
        first.replaceFileWithVersion("apkg/package-info.java", "change-annotation")
        first.compile()
        consumer.compile {
            assertCompiledSources("bpkg/UseA.kt")
        }
    }

    companion object {
        @OptIn(ExperimentalCompilerArgument::class)
        private val jsr305StrictConfigAction: (JvmCompilationOperation.Builder) -> Unit = {
            it.compilerArguments[JvmCompilerArguments.X_JSR305] = listOf(Jsr305.Global(Jsr305.Mode.STRICT))
        }
    }
}
