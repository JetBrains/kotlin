/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

fun <R : ResultingArtifact.FrontendOutput<R>> TestConfigurationBuilder.commonConfigurationForCodegenTest(
    targetFrontend: FrontendKind<*>,
    frontendFacade: Constructor<FrontendFacade<R>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, *>>,
    backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>
) {
    globalDefaults {
        frontend = targetFrontend
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +RUN_DEX_CHECKER
    }

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmEnvironmentConfigurator,
        ::ScriptingEnvironmentConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
        ::CodegenHelpersSourceFilesProvider,
    )

    useFrontendFacades(frontendFacade)
    useFrontend2BackendConverters(frontendToBackendConverter)
    useBackendFacades(backendFacade)
}

fun TestConfigurationBuilder.dumpHandlersForCodegenTest() {
    useBackendHandlers(::IrTreeVerifierHandler, ::IrTextDumpHandler)
    useArtifactsHandlers(::BytecodeListingHandler)
}

fun TestConfigurationBuilder.commonHandlersForBoxTest() {
    commonHandlersForCodegenTest()
    useArtifactsHandlers(
        ::JvmBoxRunner
    )
}

fun TestConfigurationBuilder.commonHandlersForCodegenTest() {
    useFrontendHandlers(
        ::NoCompilationErrorsHandler,
        ::NoFirCompilationErrorsHandler,
    )

    useArtifactsHandlers(
        ::NoJvmSpecificCompilationErrorsHandler,
        ::DxCheckerHandler,
    )
}

fun TestConfigurationBuilder.useInlineHandlers() {
    useArtifactsHandlers(
        ::BytecodeInliningHandler,
        ::SMAPDumpHandler
    )

    forTestsMatching("compiler/testData/codegen/boxInline/smap/*") {
        defaultDirectives {
            +DUMP_SMAP
        }
    }
}
