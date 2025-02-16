/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.KlibFacades
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.configuration.setupDefaultDirectivesForIrTextTest
import org.jetbrains.kotlin.test.configuration.setupIrTextDumpHandlers
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticsHandler
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractNonJvmIrTextTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    protected val targetPlatform: TargetPlatform,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontend: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val converter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>

    /**
     * Facades for serialization and deserialization to/from klibs.
     */
    open val klibFacades: KlibFacades?
        get() = null

    open fun TestConfigurationBuilder.applyConfigurators() {}

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        globalDefaults {
            frontend = this@AbstractNonJvmIrTextTest.frontend
            targetPlatform = this@AbstractNonJvmIrTextTest.targetPlatform
            targetBackend = this@AbstractNonJvmIrTextTest.targetBackend
            artifactKind = ArtifactKind.NoArtifact
            dependencyKind = when (targetBackend) {
                TargetBackend.JS_IR, TargetBackend.WASM -> DependencyKind.KLib // these irText pipelines register Klib artifacts during *KlibSerializerFacade
                else -> DependencyKind.Source
            }
        }

        applyConfigurators()

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        facadeStep(frontendFacade)
        classicFrontendHandlersStep {
            useHandlers(
                ::NoCompilationErrorsHandler,
                ::ClassicDiagnosticsHandler
            )
        }
        firHandlersStep {
            useHandlers(
                ::NoFirCompilationErrorsHandler,
                ::FirDiagnosticsHandler
            )
        }

        setupDefaultDirectivesForIrTextTest()
        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor,
            ::FirIrDumpIdenticalChecker,
        )
        enableMetaInfoHandler()
        useAdditionalSourceProviders(
            ::CodegenHelpersSourceFilesProvider,
        )
        facadeStep(converter)
        irHandlersStep {
            setupIrTextDumpHandlers()
        }
        klibFacades?.let {klibFacades ->
            irHandlersStep {
                useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = false) })
            }

            facadeStep(klibFacades.serializerFacade)
            klibArtifactsHandlersStep {
                this.useHandlers(::KlibAbiDumpHandler)
            }
            facadeStep(klibFacades.deserializerFacade)

            deserializedIrHandlersStep {
                useHandlers({ SerializedIrDumpHandler(it, isAfterDeserialization = true) })
            }
        }
    }
}
