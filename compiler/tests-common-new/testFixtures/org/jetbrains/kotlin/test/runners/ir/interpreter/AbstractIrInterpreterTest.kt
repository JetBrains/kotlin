/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir.interpreter

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.IrInterpreterBackendHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.preprocessors.IrInterpreterImplicitKotlinImports
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.sourceProviders.IrInterpreterHelpersSourceFilesProvider

abstract class AbstractJvmIrInterpreterTest<R : ResultingArtifact.FrontendOutput<R>>(
    private val frontendKind: FrontendKind<R>
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>

    override fun configure(builder: TestConfigurationBuilder) {
        with(builder) {
            commonConfigurationForJvmTest(frontendKind, frontendFacade, frontendToBackendConverter, backendFacade)

            globalDefaults {
                artifactKind = ArtifactKind.NoArtifact
            }

            defaultDirectives {
                +JvmEnvironmentConfigurationDirectives.FULL_JDK
                +JvmEnvironmentConfigurationDirectives.NO_RUNTIME
                +LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE
            }

            configureIrHandlersStep {
                useHandlers(::IrInterpreterBackendHandler)
            }

            useAdditionalSourceProviders(::IrInterpreterHelpersSourceFilesProvider)
            useSourcePreprocessor(::IrInterpreterImplicitKotlinImports)
            useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
            enableMetaInfoHandler()
        }
    }
}

abstract class AbstractJvmIrInterpreterAfterFir2IrTestBase(
    val parser: FirParser
) : AbstractJvmIrInterpreterTest<FirOutputArtifact>(FrontendKinds.FIR) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliJvmFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliJvmFacade

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::BackendCliJvmFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirParser(parser)
    }
}

open class AbstractJvmIrInterpreterAfterFirPsi2IrTest : AbstractJvmIrInterpreterAfterFir2IrTestBase(FirParser.Psi)

open class AbstractJvmIrInterpreterAfterPsi2IrTest : AbstractJvmIrInterpreterTest<ClassicFrontendOutputArtifact>(FrontendKinds.ClassicFrontend) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}
