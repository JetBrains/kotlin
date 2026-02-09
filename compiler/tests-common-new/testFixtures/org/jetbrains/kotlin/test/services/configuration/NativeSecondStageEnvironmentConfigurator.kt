/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.konan.config.konanProducedArtifactKind
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.CompilationStage
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.targetPlatform

class NativeSecondStageEnvironmentConfigurator(testServices: TestServices) : NativeEnvironmentConfigurator(testServices) {
    override val compilationStage: CompilationStage
        get() = CompilationStage.SECOND

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        if (!module.targetPlatform(testServices).isNative()) return

        super.configureCompilerConfiguration(configuration, module)
        configuration.konanProducedArtifactKind = CompilerOutputKind.PROGRAM
    }
}
