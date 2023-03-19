/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
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
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.runners.codegen.FirPsiCodegenTest
import org.jetbrains.kotlin.test.services.JsLibraryProvider
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JsEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.AdditionalDiagnosticsSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CodegenHelpersSourceFilesProvider
import org.jetbrains.kotlin.test.services.sourceProviders.CoroutineHelpersSourceFilesProvider

abstract class AbstractIrTextTestBase<R : ResultingArtifact.FrontendOutput<R>> :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR)
{
    abstract val frontend: FrontendKind<*>
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val converter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>

    open fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator
        )
    }

    override fun TestConfigurationBuilder.configuration() {
        globalDefaults {
            frontend = this@AbstractIrTextTestBase.frontend
            targetPlatform = JvmPlatforms.defaultJvmPlatform
            artifactKind = BinaryKind.NoArtifact
            targetBackend = TargetBackend.JVM_IR
            dependencyKind = DependencyKind.Source
        }

        defaultDirectives {
            +DUMP_IR
            +DUMP_KT_IR
        }

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
}

open class AbstractIrTextTest : AbstractIrTextTestBase<ClassicFrontendOutputArtifact>() {
    override val frontend: FrontendKind<*>
        get() = FrontendKinds.ClassicFrontend
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade
    override val converter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            useAfterAnalysisCheckers(
                ::BlackBoxCodegenSuppressor,
            )
        }
    }
}

open class AbstractFirIrTextTestBase(val parser: FirParser) : AbstractIrTextTestBase<FirOutputArtifact>() {
    override val frontend: FrontendKind<*>
        get() = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade
    override val converter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            configureFirParser(parser)
            useAfterAnalysisCheckers(
                ::FirIrDumpIdenticalChecker,
                ::BlackBoxCodegenSuppressor
            )

            forTestsMatching("compiler/fir/fir2ir/testData/ir/irText/properties/backingField/*") {
                defaultDirectives {
                    LanguageSettingsDirectives.LANGUAGE with "+ExplicitBackingFields"
                }
            }
        }
    }
}

open class AbstractFirLightTreeIrTextTest : AbstractFirIrTextTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiIrTextTest : AbstractFirIrTextTestBase(FirParser.Psi)

open class AbstractFirIrJsTextTestBase(parser: FirParser) : AbstractFirIrTextTestBase(parser) {
    override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JsEnvironmentConfigurator,
        )

        useAdditionalService(::JsLibraryProvider)
    }

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            globalDefaults {
                targetPlatform = JsPlatforms.defaultJsPlatform
                artifactKind = BinaryKind.NoArtifact
                targetBackend = TargetBackend.JS_IR
                dependencyKind = DependencyKind.Source
            }
        }
    }
}

open class AbstractFirLightTreeIrJsTextTest : AbstractFirIrJsTextTestBase(FirParser.LightTree)
