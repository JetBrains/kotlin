/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.ir

import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator

abstract class AbstractJvmIrTextTest<FrontendOutput : ResultingArtifact.FrontendOutput<FrontendOutput>> :
    AbstractIrTextTest<FrontendOutput>(JvmPlatforms.defaultJvmPlatform, TargetBackend.JVM_IR) {

    final override fun TestConfigurationBuilder.applyConfigurators() {
        useConfigurators(
            ::CommonEnvironmentConfigurator,
            ::JvmEnvironmentConfigurator
        )
    }
}

open class AbstractClassicJvmIrTextTest : AbstractJvmIrTextTest<ClassicFrontendOutputArtifact>() {

    override val frontend: FrontendKind<*>
        get() = FrontendKinds.ClassicFrontend

    override val frontendFacade: Constructor<FrontendFacade<ClassicFrontendOutputArtifact>>
        get() = ::ClassicFrontendFacade

    override val converter: Constructor<Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>>
        get() = ::ClassicFrontend2IrConverter
}
