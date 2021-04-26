/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractBytecodeTextTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    targetBackend: TargetBackend,
    val targetFrontend: FrontendKind<*>
) : AbstractKotlinCompilerWithTargetBackendTest(targetBackend) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, *>>
    abstract val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>

    override fun TestConfigurationBuilder.configuration() {
        commonConfigurationForCodegenTest(targetFrontend, frontendFacade, frontendToBackendConverter, backendFacade)

        defaultDirectives {
            +WITH_STDLIB
            +WITH_REFLECT
        }

        commonHandlersForCodegenTest()

        useArtifactsHandlers(::BytecodeTextHandler)

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}

open class AbstractBytecodeTextTest : AbstractBytecodeTextTestBase<ClassicFrontendOutputArtifact>(
    targetBackend = TargetBackend.JVM,
    targetFrontend = FrontendKinds.ClassicFrontend
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, *>>
        get() = ::ClassicFrontend2ClassicBackendConverter

    override val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>
        get() = ::ClassicJvmBackendFacade
}

open class AbstractIrBytecodeTextTest : AbstractBytecodeTextTestBase<ClassicFrontendOutputArtifact>(
    targetBackend = TargetBackend.JVM_IR,
    targetFrontend = FrontendKinds.ClassicFrontend
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, *>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}

open class AbstractFirBytecodeTextTest : AbstractBytecodeTextTestBase<FirOutputArtifact>(
    targetBackend = TargetBackend.JVM_IR,
    targetFrontend = FrontendKinds.FIR
) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, *>>
        get() = ::Fir2IrResultsConverter

    override val backendFacade: Constructor<BackendFacade<*, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                // See KT-44152
                -USE_PSI_CLASS_FILES_READING
            }
        }
    }
}
