/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.Jsr305
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
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
    fun testPackageInfoChangeCausesRecompilationInternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification").modifyPackageInfoAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changes to package-info lead to full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoChangeCausesRecompilationExternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            module(moduleName = "ic-scenarios/package-info-modification").modifyPackageInfoAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing package-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoRemovalCausesRecompilationInternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification").removePackageInfoAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Removing package-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoRemovalCausesRecompilationExternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            module(moduleName = "ic-scenarios/package-info-modification").removePackageInfoAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding package-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoAdditionCausesRecompilationInternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification").addPackageInfoAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding package-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoAdditionCausesRecompilationExternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            module(moduleName = "ic-scenarios/package-info-modification").addPackageInfoAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    private fun ScenarioModule.modifyPackageInfoAndAssertFullRecompilation(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        replaceFileWithVersion("package-info.java", "add-annotation")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(kotlinToolchains, executionPolicy),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            // If we ever decide to handle package-info.java changes in a more granular way, we can change the assertion to:
            // assertCompiledSources("a.kt")
            // because b.kt is located in a different package
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    private fun ScenarioModule.removePackageInfoAndAssertFullRecompilation(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        deleteFile("package-info.java")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(kotlinToolchains, executionPolicy),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    private fun ScenarioModule.addPackageInfoAndAssertFullRecompilation(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        deleteFile("package-info.java")
        compile()
        createPredefinedFile("package-info.java", "add-annotation")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(kotlinToolchains, executionPolicy),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A documentation-only change of package-info.java causes full recompilation (internally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoCommentOnlyChangeCausesRecompilationInternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            trackedModule(moduleName = "ic-scenarios/package-info-modification")
                .changeOnlyPackageInfoCommentAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("A documentation-only change of package-info.java causes full recompilation (externally tracked)")
    @TestMetadata("ic-scenarios/package-info-modification")
    fun testPackageInfoCommentOnlyChangeCausesRecompilationExternallyTracked(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            module(moduleName = "ic-scenarios/package-info-modification")
                .changeOnlyPackageInfoCommentAndAssertFullRecompilation(kotlinToolchains, executionPolicy)
        }
    }

    private fun ScenarioModule.changeOnlyPackageInfoCommentAndAssertFullRecompilation(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        replaceFileWithVersion("package-info.java", "comment-only")
        compile {
            assertLogContainsLines(
                expectedLogLevelForRebuildReason(kotlinToolchains, executionPolicy),
                "Non-incremental compilation will be performed: ${BuildAttribute.PACKAGE_INFO_CHANGED.readableString}"
            )
            assertCompiledSources("a.kt", "b.kt")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Adding an annotation to a dependency's package-info.java recompiles only the affected consumer source")
    @TestMetadata("ic-scenarios/dependency-package-info-modification")
    fun testDependencyPackageInfoChangeCausesConsumerRecompilation(kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-modification/module-a",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-modification/module-b",
                dependencies = listOf(dependency),
            )
            dependency.replaceFileWithVersion("package-info.java", "add-annotation")
            dependency.compile()
            consumer.compile {
                assertCompiledSources("bpkg/UseA.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing one dependency package-info recompiles only consumers of that package")
    @TestMetadata("ic-scenarios/dependency-package-info-multi-package")
    fun testDependencyPackageInfoChangeIsIsolatedToAffectedPackage(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-multi-package/module-a",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-multi-package/module-b",
                dependencies = listOf(dependency),
            )
            dependency.replaceFileWithVersion("apkg/package-info.java", "add-annotation")
            dependency.compile()
            consumer.compile {
                assertCompiledSources("bpkg/UseA.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing dependency package-info nullability recompiles the consumer and reports an error")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesConsumer(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-direct-call",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.replaceFileWithVersion("package-info.java", "add-nonnull-api")
            dependency.compile()
            consumer.compile {
                expectFailWithError(".*UseA\\.kt:\\d+:\\d+ Null cannot be a value of a non-null type 'String'.*".toRegex())
                assertCompiledSources("bpkg/UseA.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing dependency package-info nullability recompiles a consumer using fully qualified names")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesFullyQualifiedConsumer(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-fully-qualified",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.replaceFileWithVersion("package-info.java", "add-nonnull-api")
            dependency.compile()
            consumer.compile {
                expectFailWithError(".*UseAFullyQualified\\.kt:\\d+:\\d+ Null cannot be a value of a non-null type 'String'.*".toRegex())
                assertCompiledSources("bpkg/UseAFullyQualified.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Changing dependency package-info nullability recompiles a consumer that reaches the annotated type through its own factory")
    @TestMetadata("ic-scenarios/dependency-package-info-nullability")
    fun testDependencyPackageInfoNullabilityChangeRecompilesLocalFactoryConsumer(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
            val dependency = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/dependency",
                compileJavaSources = true,
            )
            val consumer = module(
                moduleName = "ic-scenarios/dependency-package-info-nullability/consumer-local-factory",
                dependencies = listOf(dependency),
                compilationConfigAction = jsr305StrictConfigAction,
            )
            dependency.replaceFileWithVersion("package-info.java", "add-nonnull-api")
            dependency.compile()
            consumer.compile {
                expectFailWithError(".*UseKotlinFactory\\.kt:\\d+:\\d+ Null cannot be a value of a non-null type 'String'.*".toRegex())
                // `bpkg/Other.kt` must stay untouched: the assertion has to distinguish a precise dirty set from a full module rebuild.
                assertCompiledSources("bpkg/KotlinFactory.kt", "bpkg/UseKotlinFactory.kt")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Only the package-info of the first classpath entry declaring the package is tracked")
    @TestMetadata("ic-scenarios/dependency-package-info-first-wins")
    fun testOnlyFirstDependencyPackageInfoIsTracked(
        kotlinToolchains: KotlinToolchains, executionPolicy: ExecutionPolicy,
    ) {
        jvmScenario(kotlinToolchains, executionPolicy) {
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
    }

    companion object {
        @OptIn(ExperimentalCompilerArgument::class)
        private val jsr305StrictConfigAction: (JvmCompilationOperation.Builder) -> Unit = {
            it.compilerArguments[JvmCompilerArguments.X_JSR305] = listOf(Jsr305.Global(Jsr305.Mode.STRICT))
        }
    }
}
