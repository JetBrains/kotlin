/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.frontend.fir.handlers.*

abstract class AbstractDiagnosticsNativeTestBase<R : ResultingArtifact.FrontendOutput<R>> : AbstractKotlinCompilerTest() {
    abstract val targetFrontend: FrontendKind<R>
    abstract val frontend: Constructor<FrontendFacade<R>>
    abstract fun handlersSetup(builder: TestConfigurationBuilder)

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = targetFrontend
            targetPlatform = NativePlatforms.unspecifiedNativePlatform
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
        }

        enableMetaInfoHandler()

        useConfigurators(::CommonEnvironmentConfigurator)

        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        facadeStep(frontend)

        handlersSetup(this)

        forTestsMatching("testData/diagnostics/nativeTests/*") {
            defaultDirectives {
                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
            }
        }
    }
}

abstract class AbstractDiagnosticsNativeTest : AbstractDiagnosticsNativeTestBase<ClassicFrontendOutputArtifact>() {
    override val targetFrontend: FrontendKind<ClassicFrontendOutputArtifact>
        get() = FrontendKinds.ClassicFrontend

    override val frontend: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override fun handlersSetup(builder: TestConfigurationBuilder) {
        builder.classicFrontendHandlersStep {
            useHandlers(
                ::DeclarationsDumpHandler,
                ::ClassicDiagnosticsHandler,
            )
        }
    }
}

abstract class AbstractFirNativeDiagnosticsTest : AbstractDiagnosticsNativeTestBase<FirOutputArtifact>() {
    override val targetFrontend: FrontendKind<FirOutputArtifact>
        get() = FrontendKinds.FIR

    override val frontend: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override fun handlersSetup(builder: TestConfigurationBuilder) {
        builder.firHandlersStep {
            useHandlers(
                ::FirDiagnosticsHandler,
                ::FirDumpHandler,
                ::FirCfgDumpHandler,
                ::FirCfgConsistencyHandler,
                ::FirResolvedTypesVerifier,
                ::FirScopeDumpHandler,
            )
        }
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)

        builder.forTestsMatching("compiler/testData/diagnostics/*") {
            configurationForClassicAndFirTestsAlongside()
        }
    }
}

abstract class AbstractFirNativeDiagnosticsWithLightTreeTest : AbstractFirNativeDiagnosticsTest() {
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +FirDiagnosticsDirectives.USE_LIGHT_TREE
            }
        }
    }
}

