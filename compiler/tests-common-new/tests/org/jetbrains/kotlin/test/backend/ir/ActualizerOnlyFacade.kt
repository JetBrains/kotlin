/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class ActualizerOnlyFacade(
    val testServices: TestServices,
) : AbstractTestFacade<IrBackendInput, IrBackendInput>() {
    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        if (module.useIrActualizer()) {
            val builtins = inputArtifact.irModuleFragment.irBuiltins
            val typeSystemContext = when (module.targetPlatform.isJvm()) {
                true -> JvmIrTypeSystemContext(builtins)
                false -> IrTypeSystemContextImpl(builtins)
            }
            IrActualizer.actualize(
                inputArtifact.irModuleFragment,
                inputArtifact.dependentIrModuleFragments,
                inputArtifact.diagnosticReporter,
                typeSystemContext,
                testServices.compilerConfigurationProvider.getCompilerConfiguration(module).languageVersionSettings
            )
        }
        return inputArtifact
    }

    private fun TestModule.useIrActualizer(): Boolean {
        return frontendKind == FrontendKinds.FIR && languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)
    }

    override val inputKind: TestArtifactKind<IrBackendInput> = BackendKinds.IrBackend
    override val outputKind: TestArtifactKind<IrBackendInput> = BackendKinds.IrBackend

    override fun shouldRunAnalysis(module: TestModule): Boolean = true
}
