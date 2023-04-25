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
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.classicFrontendHandlersStep
import org.jetbrains.kotlin.test.builders.firHandlersStep
import org.jetbrains.kotlin.test.builders.irHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractIrTextTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    private val targetPlatform: TargetPlatform,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontend: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<FrontendOutput>>
    abstract val converter: Constructor<Frontend2BackendConverter<FrontendOutput, IrBackendInput>>

    open fun TestConfigurationBuilder.applyConfigurators() {}

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = this@AbstractIrTextTest.frontend
            targetPlatform = this@AbstractIrTextTest.targetPlatform
            artifactKind = BinaryKind.NoArtifact
            targetBackend = this@AbstractIrTextTest.targetBackend
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +DUMP_IR
            +DUMP_KT_IR
        }

        useAfterAnalysisCheckers(
            ::BlackBoxCodegenSuppressor
        )

        applyConfigurators()

        useAdditionalSourceProviders(
            ::AdditionalDiagnosticsSourceFilesProvider,
            ::CoroutineHelpersSourceFilesProvider,
            ::CodegenHelpersSourceFilesProvider,
        )

        facadeStep(frontendFacade)
        classicFrontendHandlersStep {
            useHandlers(
                ::NoCompilationErrorsHandler
            )
        }

        firHandlersStep {
            useHandlers(
                ::NoFirCompilationErrorsHandler
            )
        }

        facadeStep(converter)

        irHandlersStep {
            useHandlers(
                ::IrTextDumpHandler,
                ::IrTreeVerifierHandler,
                ::IrPrettyKotlinDumpHandler
            )
        }
    }

    protected fun TestConfigurationBuilder.commonConfigurationForK2(parser: FirParser) {
        configureFirParser(parser)
        useAfterAnalysisCheckers(
            ::FirIrDumpIdenticalChecker,
        )

        forTestsMatching("compiler/testData/ir/irText/properties/backingField/*") {
            defaultDirectives {
                LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
            }
        }
    }
}

