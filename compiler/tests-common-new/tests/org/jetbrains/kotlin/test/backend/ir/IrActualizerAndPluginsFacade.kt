/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.pipeline.applyIrGenerationExtensions
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class IrActualizerAndPluginsFacade(
    val testServices: TestServices,
) : AbstractTestFacade<IrBackendInput, IrBackendInput>() {
    override fun transform(module: TestModule, inputArtifact: IrBackendInput): IrBackendInput {
        if (module.frontendKind != FrontendKinds.FIR) return inputArtifact
        if (module.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) {
            val builtins = inputArtifact.irModuleFragment.irBuiltins
            val typeSystemContext = when (module.targetPlatform.isJvm()) {
                true -> JvmIrTypeSystemContext(builtins)
                false -> IrTypeSystemContextImpl(builtins)
            }
            val result = IrActualizer.actualize(
                inputArtifact.irModuleFragment,
                inputArtifact.dependentIrModuleFragments,
                inputArtifact.diagnosticReporter,
                typeSystemContext,
                testServices.compilerConfigurationProvider.getCompilerConfiguration(module).languageVersionSettings,
                inputArtifact.fir2IrComponents!!.symbolTable,
                inputArtifact.fir2IrComponents!!.fakeOverrideBuilder,
                useIrFakeOverrideBuilder = CodegenTestDirectives.ENABLE_IR_FAKE_OVERRIDE_GENERATION in module.directives,
                expectActualTracker = null,
            )
            inputArtifact.irActualizerResult = result
        }
        inputArtifact.irPluginContext.applyIrGenerationExtensions(
            inputArtifact.irModuleFragment,
            irGenerationExtensions = module.irGenerationExtensions(testServices)
        )
        return inputArtifact
    }

    private fun TestModule.irGenerationExtensions(testServices: TestServices): Collection<IrGenerationExtension> {
        return IrGenerationExtension.getInstances(testServices.compilerConfigurationProvider.getProject(this))
    }

    override val inputKind: TestArtifactKind<IrBackendInput> = BackendKinds.IrBackend
    override val outputKind: TestArtifactKind<IrBackendInput> = BackendKinds.IrBackend

    override fun shouldRunAnalysis(module: TestModule): Boolean = true
}
