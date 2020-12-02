/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.TestServices

class JvmIrBackendFacade(
    testServices: TestServices
) : IrBackendFacade<BinaryArtifacts.Jvm>(testServices, ArtifactKinds.Jvm) {
    override fun transform(
        module: TestModule,
        inputArtifact: IrBackendInput
    ): BinaryArtifacts.Jvm {
        val (state, irModuleFragment, symbolTable, sourceManager, phaseConfig, irProviders, extensions, serializerFactory) = inputArtifact

        val codegenFactory = state.codegenFactory as JvmIrCodegenFactory
        codegenFactory.doGenerateFilesInternal(
            state,
            irModuleFragment,
            symbolTable,
            sourceManager,
            phaseConfig,
            irProviders,
            extensions,
            serializerFactory
        )
        state.factory.done()

        return BinaryArtifacts.Jvm(state.factory)
    }
}
