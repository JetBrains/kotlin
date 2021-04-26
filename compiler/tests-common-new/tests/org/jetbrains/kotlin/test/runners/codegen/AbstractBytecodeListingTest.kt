/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.handlers.BytecodeListingHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest
import org.jetbrains.kotlin.test.frontend.classic.*
import org.jetbrains.kotlin.test.model.*

abstract class AbstractBytecodeListingTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    targetBackend: TargetBackend,
    val targetFrontend: FrontendKind<*>
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, *>>
    abstract val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForCodegenTest(targetFrontend, frontendFacade, frontendToBackendConverter, backendFacade)
        commonHandlersForCodegenTest()
        defaultDirectives {
            +CodegenTestDirectives.CHECK_BYTECODE_LISTING
        }
        useArtifactsHandlers(::BytecodeListingHandler)
        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}

open class AbstractBytecodeListingTest : AbstractBytecodeListingTestBase<ClassicFrontendOutputArtifact>(
    TargetBackend.JVM, FrontendKinds.ClassicFrontend
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, *>>
        get() = ::ClassicFrontend2ClassicBackendConverter

    override val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>
        get() = ::ClassicJvmBackendFacade
}

open class AbstractIrBytecodeListingTest : AbstractBytecodeListingTestBase<ClassicFrontendOutputArtifact>(
    TargetBackend.JVM_IR, FrontendKinds.ClassicFrontend
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, *>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}
