/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.common.BackendException
import org.jetbrains.kotlin.test.backend.classic.JavaCompilerFacade
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

class JvmIrBackendFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.Jvm>(testServices, ArtifactKinds.Jvm) {
    private val javaCompilerFacade = JavaCompilerFacade(testServices)

    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput
    ): BinaryArtifacts.Jvm? {
        require(inputArtifact is IrBackendInput.JvmIrBackendInput) {
            "JvmIrBackendFacade expects IrBackendInput.JvmIrBackendInput as input"
        }
        val state = inputArtifact.state
        try {
            inputArtifact.codegenFactory.generateModule(state, inputArtifact.backendInput)
        } catch (e: BackendException) {
            if (CodegenTestDirectives.IGNORE_ERRORS in module.directives) {
                return null
            }
            throw e
        }
        state.factory.done()
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        javaCompilerFacade.compileJavaFiles(module, configuration, state.factory)

        return BinaryArtifacts.Jvm(state.factory, inputArtifact.backendInput.ktFiles)
    }
}
