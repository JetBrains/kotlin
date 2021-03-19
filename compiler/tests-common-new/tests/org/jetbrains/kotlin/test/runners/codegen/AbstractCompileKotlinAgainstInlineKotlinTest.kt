/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.classic.ClassicBackendInput
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.bind
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.IGNORE_BACKEND_MULTI_MODULE
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.ModuleTransformerForTwoFilesBoxTests
import org.jetbrains.kotlin.test.services.TestServices


@OptIn(TestInfrastructureInternals::class)
abstract class AbstractCompileKotlinAgainstInlineKotlinTestBase<I : ResultingArtifact.BackendInput<I>>(
    targetBackend: TargetBackend
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, I>>
    abstract val backendFacade: Constructor<BackendFacade<I, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() = configurationImpl()

    protected fun TestConfigurationBuilder.configurationImpl() {
        commonConfigurationForCodegenTest(
            FrontendKinds.ClassicFrontend,
            ::ClassicFrontendFacade,
            frontendToBackendConverter,
            backendFacade
        )
        useInlineHandlers()
        commonHandlersForBoxTest()
        useModuleStructureTransformers(
            ModuleTransformerForTwoFilesBoxTests()
        )
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor.bind(IGNORE_BACKEND_MULTI_MODULE))
    }
}

open class AbstractCompileKotlinAgainstInlineKotlinTest :
    AbstractCompileKotlinAgainstInlineKotlinTestBase<ClassicBackendInput>(TargetBackend.JVM) {
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, ClassicBackendInput>>
        get() = ::ClassicFrontend2ClassicBackendConverter

    override val backendFacade: Constructor<BackendFacade<ClassicBackendInput, BinaryArtifacts.Jvm>>
        get() = ::ClassicJvmBackendFacade
}

open class AbstractIrCompileKotlinAgainstInlineKotlinTest :
    AbstractCompileKotlinAgainstInlineKotlinTestBase<IrBackendInput>(TargetBackend.JVM_IR) {
    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}

open class AbstractIrSerializeCompileKotlinAgainstInlineKotlinTest : AbstractIrCompileKotlinAgainstInlineKotlinTest() {
    override fun TestConfigurationBuilder.configuration() {
        // call super
        configurationImpl()
        useConfigurators(::SerializeSetter)
    }

    private class SerializeSetter(testServices: TestServices) : EnvironmentConfigurator(testServices) {
        override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
            configuration.put(JVMConfigurationKeys.SERIALIZE_IR, true)
        }
    }
}