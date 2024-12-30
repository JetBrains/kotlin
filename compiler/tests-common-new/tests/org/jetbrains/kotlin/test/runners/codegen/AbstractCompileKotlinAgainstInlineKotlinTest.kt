/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeListingHandler
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.backend.handlers.IrInlineBodiesHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureIrHandlersStep
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_K2_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.SERIALIZE_IR
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.LINK_VIA_SIGNATURES_K1
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.SplittingModuleTransformerForBoxTests
import org.jetbrains.kotlin.utils.bind

@OptIn(TestInfrastructureInternals::class)
abstract class AbstractCompileKotlinAgainstInlineKotlinTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>
    open val directiveToSuppressTest: ValueDirective<TargetBackend> = IGNORE_BACKEND_MULTI_MODULE

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForTest(targetFrontend, frontendFacade, frontendToBackendConverter)
        useInlineHandlers()
        configureCommonHandlersForBoxTest()
        useModuleStructureTransformers(
            SplittingModuleTransformerForBoxTests()
        )
        configureJvmArtifactsHandlersStep {
            useHandlers(
                ::BytecodeTextHandler.bind(true)
            )
        }
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor.bind(directiveToSuppressTest))
    }
}

open class AbstractIrCompileKotlinAgainstInlineKotlinTest(targetBackend: TargetBackend = TargetBackend.JVM_IR) :
    AbstractCompileKotlinAgainstInlineKotlinTestBase<ClassicFrontendOutputArtifact>(
        FrontendKinds.ClassicFrontend,
        targetBackend
    ) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter
}

private fun TestConfigurationBuilder.configureForSerialization() {
    defaultDirectives {
        SERIALIZE_IR.with(JvmSerializeIrMode.INLINE)
        +LINK_VIA_SIGNATURES_K1
    }

    configureIrHandlersStep {
        useHandlers(::IrInlineBodiesHandler)
    }
}

open class AbstractIrSerializeCompileKotlinAgainstInlineKotlinTest :
    AbstractIrCompileKotlinAgainstInlineKotlinTest(TargetBackend.JVM_IR_SERIALIZE)
{
    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForSerialization()
    }
}

open class AbstractFirSerializeCompileKotlinAgainstInlineKotlinTestBase(val parser: FirParser) :
    AbstractCompileKotlinAgainstInlineKotlinTestBase<FirOutputArtifact>(FrontendKinds.FIR, TargetBackend.JVM_IR_SERIALIZE) {

    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrResultsConverter

    override val directiveToSuppressTest = IGNORE_BACKEND_K2_MULTI_MODULE

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        builder.configureForSerialization()
        builder.configureFirParser(parser)
    }
}

open class AbstractFirLightTreeSerializeCompileKotlinAgainstInlineKotlinTest :
    AbstractFirSerializeCompileKotlinAgainstInlineKotlinTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiSerializeCompileKotlinAgainstInlineKotlinTest :
    AbstractFirSerializeCompileKotlinAgainstInlineKotlinTestBase(FirParser.Psi)
