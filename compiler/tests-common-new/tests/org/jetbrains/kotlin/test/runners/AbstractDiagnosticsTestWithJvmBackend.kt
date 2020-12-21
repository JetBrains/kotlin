/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.handlers.JvmBackendDiagnosticsHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.handlers.ClassicDiagnosticsHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.DeclarationsDumpHandler
import org.jetbrains.kotlin.test.frontend.classic.handlers.OldNewInferenceMetaInfoProcessor
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.CoroutineHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

abstract class AbstractDiagnosticsTestWithJvmBackend : AbstractKotlinCompilerTest() {
    abstract val targetBackendKind: BackendKind<*>

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = FrontendKinds.ClassicFrontend
            backend = targetBackendKind
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            dependencyKind = DependencyKind.Binary
        }

        defaultDirectives {
            +JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
        }

        enableMetaInfoHandler()

        useConfigurators(::JvmEnvironmentConfigurator)

        useMetaInfoProcessors(::OldNewInferenceMetaInfoProcessor)
        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
        )

        useFrontendFacades(::ClassicFrontendFacade)

        useFrontendHandlers(
            ::DeclarationsDumpHandler,
            ::ClassicDiagnosticsHandler,
        )

        useFrontend2BackendConverters(
            ::ClassicFrontend2ClassicBackendConverter,
            ::ClassicFrontend2IrConverter
        )

        useBackendFacades(
            ::ClassicJvmBackendFacade,
            ::JvmIrBackendFacade
        )

        useArtifactsHandlers(
            ::JvmBackendDiagnosticsHandler
        )
    }
}

abstract class AbstractDiagnosticsTestWithOldJvmBackend : AbstractDiagnosticsTestWithJvmBackend() {
    override val targetBackendKind: BackendKind<*>
        get() = BackendKinds.ClassicBackend
}

abstract class AbstractDiagnosticsTestWithJvmIrBackend : AbstractDiagnosticsTestWithJvmBackend() {
    override val targetBackendKind: BackendKind<*>
        get() = BackendKinds.IrBackend
}
