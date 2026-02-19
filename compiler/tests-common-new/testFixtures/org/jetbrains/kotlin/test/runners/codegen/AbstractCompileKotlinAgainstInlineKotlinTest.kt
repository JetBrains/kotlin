/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.configureCommonHandlersForBoxTest
import org.jetbrains.kotlin.test.configuration.useInlineHandlers
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.directives.model.ValueDirective
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.SplittingModuleTransformerForBoxTests
import org.jetbrains.kotlin.test.services.SplittingTestConfigurator
import org.jetbrains.kotlin.utils.bind

@OptIn(TestInfrastructureInternals::class)
abstract class AbstractCompileKotlinAgainstInlineKotlinTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>,
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
    open val directiveToSuppressTest: ValueDirective<TargetBackend> = IGNORE_BACKEND_MULTI_MODULE

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        commonConfigurationForJvmTest(targetFrontend, frontendFacade, frontendToBackendConverter, backendFacade)
        useInlineHandlers()
        configureCommonHandlersForBoxTest()
        useModuleStructureTransformers(
            ::SplittingModuleTransformerForBoxTests
        )
        useMetaTestConfigurators(::SplittingTestConfigurator)
        configureJvmArtifactsHandlersStep {
            useHandlers(
                ::BytecodeTextHandler.bind(true)
            )
        }
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor.bind(null, listOf(directiveToSuppressTest)))
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

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}
