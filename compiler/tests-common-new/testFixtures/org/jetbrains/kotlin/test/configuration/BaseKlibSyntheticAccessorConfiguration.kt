/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.configuration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.NoIrCompilationErrorsHandler
import org.jetbrains.kotlin.test.backend.handlers.SyntheticAccessorsDumpHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives.DIAGNOSTICS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LANGUAGE
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.fir.FirMetaInfoDiffSuppressor
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgConsistencyHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirCfgDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDumpHandler
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.LibraryProvider
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.utils.bind

fun TestConfigurationBuilder.commonConfigurationForDumpSyntheticAccessorsTest(
    targetFrontend: FrontendKind<FirOutputArtifact>,
    frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>,
    frontendToIrConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>,
    irInliningFacade: Constructor<IrPreSerializationLoweringFacade<IrBackendInput>>,
    serializerFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.KLib>>,
    deserializerFacade: Constructor<org.jetbrains.kotlin.test.model.DeserializerFacade<BinaryArtifacts.KLib, IrBackendInput>>,
    customIgnoreDirective: ValueDirective<TargetBackend>? = null,
) {
    globalDefaults {
        frontend = targetFrontend
        dependencyKind = DependencyKind.Binary
    }
    defaultDirectives {
        DIAGNOSTICS with listOf("-NOTHING_TO_INLINE", "-ERROR_SUPPRESSION", "-UNCHECKED_CAST")
        LANGUAGE with listOf(
            "+${LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization.name}",
            "+${LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization.name}"
        )
        +DiagnosticsDirectives.REPORT_ONLY_EXPLICITLY_DEFINED_DEBUG_INFO
        +ConfigurationDirectives.WITH_STDLIB
    }
    useAdditionalService(::LibraryProvider)
    useAdditionalSourceProviders(
        ::CoroutineHelpersSourceFilesProvider,
        ::AdditionalDiagnosticsSourceFilesProvider,
    )
    useAfterAnalysisCheckers(
        ::BlackBoxCodegenSuppressor.bind(customIgnoreDirective, null),
        ::FirMetaInfoDiffSuppressor,
    )
    facadeStep(frontendFacade)
    firHandlersStep {
        commonFirHandlersForCodegenTest()
        useHandlers(
            ::FirDumpHandler,
            ::FirCfgDumpHandler,
            ::FirCfgConsistencyHandler,
            ::FirResolvedTypesVerifier,
        )
    }
    facadeStep(frontendToIrConverter)
    irHandlersStep {
        useHandlers(::NoIrCompilationErrorsHandler)
    }
    facadeStep(irInliningFacade)
    loweredIrHandlersStep {
        useHandlers(::NoIrCompilationErrorsHandler)
    }

    enableMetaInfoHandler()
    facadeStep(serializerFacade)
    facadeStep(deserializerFacade)
    deserializedIrHandlersStep { useHandlers(::SyntheticAccessorsDumpHandler) }
}

