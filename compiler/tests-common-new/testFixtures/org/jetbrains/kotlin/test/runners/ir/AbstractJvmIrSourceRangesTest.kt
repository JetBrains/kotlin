/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_KT_IR
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SOURCE_RANGES_IR
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*

abstract class AbstractJvmIrSourceRangesTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>>(
    targetFrontend: FrontendKind<FrontendOutput>
) : AbstractJvmIrTextTest<FrontendOutput>(targetFrontend) {

    override fun configure(builder: TestConfigurationBuilder) {
        super.configure(builder)
        with(builder) {
            defaultDirectives {
                +DUMP_SOURCE_RANGES_IR
                -DUMP_KT_IR
                -DUMP_IR
            }
        }
    }
}

open class AbstractClassicJvmIrSourceRangesTest : AbstractJvmIrSourceRangesTest<ClassicFrontendOutputArtifact>(FrontendKinds.ClassicFrontend) {
    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val frontendToBackendConverter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter

    override val backendFacade: Constructor<BackendFacade<IrBackendInput, BinaryArtifacts.Jvm>>
        get() = ::JvmIrBackendFacade
}
