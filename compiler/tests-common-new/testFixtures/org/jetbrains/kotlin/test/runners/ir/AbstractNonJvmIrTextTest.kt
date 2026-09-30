/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.AbstractKlibAbiDumpBeforeInliningSavingHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTextDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.IrTreeVerifierHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpAfterInliningVerifyingHandler
import org.jetbrains.kotlin.test.backend.handlers.KlibAbiDumpHandler
import org.jetbrains.kotlin.test.backend.handlers.SerializedIrDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.IrDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.commonFirHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.commonIrHandlersForCodegenTest
import org.jetbrains.kotlin.test.configuration.setupDefaultDirectivesForIrDumps
import org.jetbrains.kotlin.test.configuration.setupDefaultDirectivesForIrTextTest
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
import org.jetbrains.kotlin.test.directives.TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.UnspecifiedTargetBackend
import org.jetbrains.kotlin.test.services.PhasedPipelineChecker
import org.jetbrains.kotlin.test.services.TestPhase
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInCommonBackend
import org.jetbrains.kotlin.utils.bind

@OptIn(UnspecifiedTargetBackend::class)
@MustRunOnChangesInCommonBackend
abstract class AbstractNonJvmIrTextTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    protected val targetPlatform: TargetPlatform,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontend: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val converter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>
    abstract val preSerializerFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>

    /**
     * Facades for serialization and deserialization to/from klibs.
     */
    abstract val klibFacades: KlibFacades

    open val klibAbiDumpBeforeInliningSavingHandler: Constructor<AbstractKlibAbiDumpBeforeInliningSavingHandler>?
        get() = null

    /** Dump first-stage IR while retaining klib serialization for module dependencies. */
    open val irDumpOnly: Boolean
        get() = false

    open fun TestConfigurationBuilder.applyConfigurators() {}

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        globalDefaults {
            frontend = this@AbstractNonJvmIrTextTest.frontend
            targetPlatform = this@AbstractNonJvmIrTextTest.targetPlatform
            targetBackend = this@AbstractNonJvmIrTextTest.targetBackend
            artifactKind = ArtifactKind.NoArtifact
            dependencyKind = DependencyKind.Binary
        }

        applyConfigurators()

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        facadeStep(frontendFacade)
        firHandlersStep {
            commonFirHandlersForCodegenTest()
            useHandlers(
                ::FirDiagnosticsHandler
            )
        }

        if (irDumpOnly) {
            setupDefaultDirectivesForIrDumps()
            defaultDirectives {
                +REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
                DIAGNOSTICS with "-warnings"
                LATEST_PHASE_IN_PIPELINE with TestPhase.CODEGEN
            }
        } else {
            setupDefaultDirectivesForIrTextTest()
        }
        useFailureSuppressors(
            ::BlackBoxCodegenSuppressor,
            ::PhasedPipelineChecker.bind(TestPhase.CODEGEN)
        )
        enableMetaInfoHandler()
        facadeStep(converter)
        irHandlersStep {
            commonIrHandlersForCodegenTest()
            if (irDumpOnly) {
                useHandlers(::IrTextDumpHandler, ::IrTreeVerifierHandler)
            } else {
                setupIrTextDumpHandlers()
                klibAbiDumpBeforeInliningSavingHandler?.let {
                    useHandlers(it)
                }
            }
        }
        facadeStep(preSerializerFacade)

        if (!irDumpOnly) {
            loweredIrHandlersStep {
                useHandlers(::IrDiagnosticsHandler, { SerializedIrDumpHandler(it, isAfterDeserialization = false) })
            }
        }

        facadeStep(klibFacades.serializerFacade)
        if (!irDumpOnly) {
            klibArtifactsHandlersStep {
                useHandlers(::KlibAbiDumpHandler)
                klibAbiDumpBeforeInliningSavingHandler?.run {
                    useHandlers(::KlibAbiDumpAfterInliningVerifyingHandler)
                }
            }
        }
        if (!irDumpOnly) {
            facadeStep(klibFacades.deserializerFacade)

            deserializedIrHandlersStep {
                useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) })
            }
        }
    }
}
