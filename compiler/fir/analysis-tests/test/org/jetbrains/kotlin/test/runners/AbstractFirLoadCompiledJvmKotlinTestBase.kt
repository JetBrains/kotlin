/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*

abstract class AbstractFirLoadCompiledJvmKotlinTestBase<F : ResultingArtifact.FrontendOutput<F>> :
    AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR)
{
    protected abstract val frontendKind: FrontendKind<F>
    protected abstract val frontendFacade: Constructor<FrontendFacade<F>>
    protected abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, IrBackendInput>>
    protected abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>

    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        commonConfigurationForJvmTest(frontendKind, frontendFacade, frontendToBackendConverter, backendFacade)

        configureJvmArtifactsHandlersStep {
            useHandlers(::JvmLoadedMetadataDumpHandler)
        }

        forTestsMatching("compiler/testData/loadJava/compiledKotlinWithStdlib/*") {
            defaultDirectives {
                +WITH_STDLIB
            }
        }

        useAfterAnalysisCheckers(::FirMetadataLoadingTestSuppressor)
    }
}

open class AbstractFirLoadK1CompiledJvmKotlinTest : AbstractFirLoadCompiledJvmKotlinTestBase<ClassicFrontendOutputArtifact>() {
    override val frontendKind: FrontendKind<ClassicFrontendOutputArtifact>
        get() = FrontendKinds.ClassicFrontend
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter
    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}

open class AbstractFirLoadK2CompiledJvmKotlinTest : AbstractFirLoadCompiledJvmKotlinTestBase<FirOutputArtifact>() {
    override val frontendKind: FrontendKind<FirOutputArtifact>
        get() = FrontendKinds.FIR
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliJvmFacade
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliJvmFacade
    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::BackendCliJvmFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureFirParser(FirParser.LightTree)
    }
}

