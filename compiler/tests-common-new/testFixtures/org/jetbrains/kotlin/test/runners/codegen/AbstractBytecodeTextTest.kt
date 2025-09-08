/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.BlackBoxCodegenSuppressor
import org.jetbrains.kotlin.test.backend.handlers.BytecodeTextHandler
import org.jetbrains.kotlin.test.backend.ir.BackendCliJvmFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.commonConfigurationForJvmTest
import org.jetbrains.kotlin.test.configuration.commonHandlersForCodegenTest
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives.WITH_STDLIB
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.USE_PSI_CLASS_FILES_READING
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives.WITH_REFLECT
import org.jetbrains.kotlin.test.directives.configureFirParser
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirCliJvmFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerWithTargetBackendTest

abstract class AbstractBytecodeTextTestBase<R : ResultingArtifact.FrontendOutput<R>>(
    val targetFrontend: FrontendKind<R>
) : AbstractKotlinCompilerWithTargetBackendTest(TargetBackend.JVM_IR) {
    abstract val frontendFacade: Constructor<FrontendFacade<R>>
    abstract val frontendToBackendConverter: Constructor<Frontend2BackendConverter<R, IrBackendInput>>
    abstract val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>

    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        defaultDirectives {
            +WITH_STDLIB
            +WITH_REFLECT
        }

        commonConfigurationForJvmTest(targetFrontend, frontendFacade, frontendToBackendConverter, backendFacade)

        commonHandlersForCodegenTest()

        configureJvmArtifactsHandlersStep {
            useHandlers(::BytecodeTextHandler)
        }

        useAfterAnalysisCheckers(::BlackBoxCodegenSuppressor)
    }
}

open class AbstractIrBytecodeTextTest : AbstractBytecodeTextTestBase<ClassicFrontendOutputArtifact>(
    targetFrontend = FrontendKinds.ClassicFrontend
) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}

open class AbstractFirBytecodeTextTestBase(val parser: FirParser) : AbstractBytecodeTextTestBase<FirOutputArtifact>(
    targetFrontend = FrontendKinds.FIR
) {
    override val frontendFacade: Constructor<FrontendFacade<FirOutputArtifact>>
        get() = ::FirCliJvmFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>>
        get() = ::Fir2IrCliJvmFacade

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::BackendCliJvmFacade

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                // See KT-44152
                -USE_PSI_CLASS_FILES_READING
                builder.configureFirParser(parser)
            }
        }
    }
}

open class AbstractFirLightTreeBytecodeTextTest : AbstractFirBytecodeTextTestBase(FirParser.LightTree)

@FirPsiCodegenTest
open class AbstractFirPsiBytecodeTextTest : AbstractFirBytecodeTextTestBase(FirParser.Psi)
