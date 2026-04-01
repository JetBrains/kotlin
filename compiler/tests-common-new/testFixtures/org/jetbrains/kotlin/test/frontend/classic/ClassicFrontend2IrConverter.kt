/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class ClassicFrontend2IrConverter(
    testServices: TestServices
) : Frontend2BackendConverter<ClassicFrontendOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.ClassicFrontend,
    BackendKinds.IrBackend
) {
    override fun transform(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        return when (val targetBackend = testServices.defaultsProvider.targetBackend) {
            TargetBackend.JVM_IR, TargetBackend.JVM_IR_SERIALIZE -> transformToJvmIr(module, inputArtifact)
            else -> testServices.assertions.fail { "Target backend $targetBackend not supported for transformation into IR" }
        }
    }

    private fun transformToJvmIr(module: TestModule, inputArtifact: ClassicFrontendOutputArtifact): IrBackendInput {
        val (psiFiles, analysisResult, project, _) = inputArtifact

        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val codegenFactory = JvmIrCodegenFactory(configuration)
        val state = GenerationState(
            project, analysisResult.moduleDescriptor, configuration, ClassBuilderFactories.TEST,
            ignoreErrors = CodegenTestDirectives.IGNORE_ERRORS in module.directives,
        )

        val backendInput = codegenFactory.convertToIr(state, psiFiles.values, analysisResult.bindingContext)
        return IrBackendInput.JvmIrBackendInput(
            state,
            codegenFactory,
            backendInput,
            sourceFiles = emptyList(),
            descriptorMangler = backendInput.symbolTable.signaturer!!.mangler,
            irMangler = JvmIrMangler,
        )
    }
}
