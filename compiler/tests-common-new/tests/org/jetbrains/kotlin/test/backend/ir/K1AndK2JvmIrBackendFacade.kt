/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.runWithEnablingFirUseOption
import org.jetbrains.kotlin.test.services.TestServices

class K1AndK2JvmIrBackendFacade(testServices: TestServices) :
    BackendFacade<IrBackendInputsFromK1AndK2, BinaryArtifacts.JvmFromK1AndK2>(
        testServices,
        BackendKinds.IrBackendForK1AndK2,
        ArtifactKinds.JvmFromK1AndK2
    ) {

    private val backendForClassicFrontend = JvmIrBackendFacade(testServices)
    private val backendForFir = JvmIrBackendFacade(testServices)

    override fun transform(module: TestModule, inputArtifact: IrBackendInputsFromK1AndK2): BinaryArtifacts.JvmFromK1AndK2? {
        val fromClassicFrontend = backendForClassicFrontend.transform(module, inputArtifact.fromK1) ?: return null
        val fromFir =
            runWithEnablingFirUseOption(testServices, module) { backendForFir.transform(module, inputArtifact.fromK2) } ?: return null
        return BinaryArtifacts.JvmFromK1AndK2(fromClassicFrontend, fromFir)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == BackendKinds.IrBackend && module.binaryKind == ArtifactKinds.JvmFromK1AndK2
    }
}