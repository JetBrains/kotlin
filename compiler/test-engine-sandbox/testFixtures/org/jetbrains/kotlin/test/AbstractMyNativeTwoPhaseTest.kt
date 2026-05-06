/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.builtins.StandardNames.KOTLIN_INTERNAL_FQ_NAME
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.konan.test.KlibSerializerNativeCliFacade
import org.jetbrains.kotlin.konan.test.blackbox.support.AssertionsMode
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.computeBlackBoxTestInstances
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.createTestRunSettings
import org.jetbrains.kotlin.konan.test.blackbox.support.NativeTestSupport.getOrCreateTestRunProvider
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.FILECHECK_STAGE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives.NATIVE_STANDALONE
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.testKind
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.konan.test.configuration.commonConfigurationForNativeCodegenTest
import org.jetbrains.kotlin.konan.test.configuration.setupStepsForNativeFirstStageUpToSerialization
import org.jetbrains.kotlin.konan.test.handlers.NativeBoxRunnerGroupingPhase
import org.jetbrains.kotlin.konan.test.klib.NativeCompilerSecondStageFacade
import org.jetbrains.kotlin.konan.test.klib.currentCustomNativeCompilerSettings
import org.jetbrains.kotlin.konan.test.services.CInteropTestSkipper
import org.jetbrains.kotlin.konan.test.services.DisabledNativeTestSkipper
import org.jetbrains.kotlin.konan.test.services.FileCheckTestSkipper
import org.jetbrains.kotlin.konan.test.services.sourceProviders.NativeLauncherAdditionalSourceProvider
import org.jetbrains.kotlin.konan.test.suppressors.NativeTestsSuppressor
import org.jetbrains.kotlin.test.backend.handlers.NoFirCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.OPT_IN
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.GroupingTestIsolator
import org.jetbrains.kotlin.test.model.SimpleTestFailureSuppressor
import org.jetbrains.kotlin.test.services.BatchingPackageInserter
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeFirstStageEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.NativeSecondStageEnvironmentConfigurator
import org.jetbrains.kotlin.utils.bind
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension

abstract class AbstractMyNativeTwoPhaseTest : AbstractTwoStageKotlinCompilerTest() {
    private lateinit var extensionContext: ExtensionContext

    @RegisterExtension
    val extensionContextCaptor = BeforeEachCallback { context ->
        this.extensionContext = context
    }

    override fun configure(builder: TwoPhaseTestConfigurationBuilder): Unit = with(builder) {
        commonConfiguration {
            defaultDirectives {
                LANGUAGE with listOf(
                    "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
                    "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
                )
                OPT_IN with listOf(
                    "kotlin.native.internal.InternalForKotlinNative",
                    "kotlin.native.internal.InternalForKotlinNativeTests",
                    "kotlin.experimental.ExperimentalNativeApi"
                )
            }

            commonConfigurationForNativeCodegenTest()

            useMetaTestConfigurators(::DisabledNativeTestSkipper, ::CInteropTestSkipper, ::FileCheckTestSkipper)
            useFailureSuppressors(
                ::FirMetaInfoDiffSuppressor,
                ::NativeTestsSuppressor,
            )

            // TODO(KT-84712): should be moved into `AbstractTwoStageKotlinCompilerTest`
            useSourcePreprocessor(::BatchingPackageInserter)

            useAdditionalService { // Register TestRunSettings into TestServices
                extensionContext.createTestRunSettings(extensionContext.computeBlackBoxTestInstances())
            }
            useAdditionalService { // Register TestRunProvider into TestServices
                extensionContext.getOrCreateTestRunProvider()
            }
        }

        nonGroupingPhase {
            useConfigurators(
                ::CommonEnvironmentConfigurator,
                ::NativeFirstStageEnvironmentConfigurator,
            )

            useGroupingTestIsolators(::NativeGroupingTestIsolator, ::MutedTestsIsolator)

            // Because of package escaping various dumps for grouping mode would be different from
            // the regular one, so we don't want all the frontend handlers to be set up, only some specific ones.
            setupStepsForNativeFirstStageUpToSerialization(
                includeBasicFirHandlers = false,
                includeDumpFirHandlers = false
            )

            configureFirHandlersStep {
                useHandlers(::FirDiagnosticsHandler, ::NoFirCompilationErrorsHandler)
            }

            configureIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            configureLoweredIrHandlersStep {
                useHandlers(::NoIrCompilationErrorsHandler)
            }

            facadeStep(::KlibSerializerNativeCliFacade)
            klibArtifactsHandlersStep()

            useAdditionalSourceProviders(
                ::NativeLauncherAdditionalSourceProvider,
            )

            forTestsNotMatching(
                "compiler/testData/codegen/box/diagnostics/functions/tailRecursion/*" or
                        "compiler/testData/diagnostics/*"
            ) {
                defaultDirectives {
                    DIAGNOSTICS with "-warnings"
                }
            }
            enableMetaInfoHandler()
        }

        groupingPhase {
            useConfigurators(::NativeSecondStageEnvironmentConfigurator)

            facadeStep(NativeCompilerSecondStageFacade::Grouping.bind(currentCustomNativeCompilerSettings))
            handlersStep(ArtifactKinds.Native, CompilationStage.SECOND) {
                useHandlers(::NativeBoxRunnerGroupingPhase)
            }
        }
    }
}

class NativeGroupingTestIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = true) {
    companion object {
        private val assertionTokens = AssertionsMode.entries.associateWith {
            BatchToken.Custom("AssertionMode: ${it.name}")
        }
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(TestDirectives)

    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        // KT-84713: Migrate here full grouping logic from TestRunProvider.withTestExecutable(): respect ignores, difference of compiler args, etc.
        val shouldBeIsolated = testServices.testRunSettings.testKind(moduleStructure.modules.firstOrNull()?.directives) != TestKind.REGULAR
                || moduleStructure.allDirectives.contains(NATIVE_STANDALONE)
                || moduleStructure.allDirectives[FILECHECK_STAGE].isNotEmpty()
                || moduleStructure.sourceContains(packageKotlinInternalRegex)
        if (shouldBeIsolated) return BatchToken.Isolated
        return computeAssertionsModeToken(moduleStructure) ?: BatchToken.Regular
    }

    private fun computeAssertionsModeToken(moduleStructure: TestModuleStructure): BatchToken? {
        val assertionsMode = moduleStructure.allDirectives.singleOrZeroValue(TestDirectives.ASSERTIONS_MODE) ?: return null
        return assertionTokens.getValue(assertionsMode)
    }

    private val packageKotlinInternalRegex = Regex("package\\s$KOTLIN_INTERNAL_FQ_NAME")
    private val sourceContainsCache = HashMap<Pair<TestModuleStructure, Regex>, Boolean>()

    private fun TestModuleStructure.sourceContains(regex: Regex): Boolean {
        return sourceContainsCache.getOrPut(this to regex) { modules.any { it.files.any { it.originalContent.contains(regex) } } }
    }
}

class MutedTestsIsolator(testServices: TestServices) : GroupingTestIsolator(testServices, affectsFileGenerators = false) {
    override fun computeBatchToken(moduleStructure: TestModuleStructure): BatchToken {
        @OptIn(TestInfrastructureInternals::class)
        val testIsMuted = testServices.testConfiguration.failureSuppressors.filterIsInstance<SimpleTestFailureSuppressor>().any { it.testIsMuted() }
        if (testIsMuted) return BatchToken.Isolated
        return BatchToken.Regular
    }
}
