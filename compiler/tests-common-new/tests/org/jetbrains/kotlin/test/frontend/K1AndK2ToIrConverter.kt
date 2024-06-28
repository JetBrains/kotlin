/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend

import org.jetbrains.kotlin.test.backend.ir.IrBackendInputsFromK1AndK2
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrJvmResultsConverter
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runWithEnablingFirUseOption
import org.jetbrains.kotlin.test.services.TestServices

class K1AndK2ToIrConverter(testServices: TestServices) :
    Frontend2BackendConverter<K1AndK2OutputArtifact, IrBackendInputsFromK1AndK2>(
        testServices,
        FrontendKinds.ClassicAndFIR,
        BackendKinds.IrBackendForK1AndK2
    ) {

    private val classicFrontend2IrConverter = ClassicFrontend2IrConverter(testServices)
    private val fir2IrConverter = Fir2IrJvmResultsConverter(testServices)
    override fun transform(module: TestModule, inputArtifact: K1AndK2OutputArtifact): IrBackendInputsFromK1AndK2? {
        val irFromClassic = classicFrontend2IrConverter.transform(module, inputArtifact.k1Artifact)
        val irFromFir =
            runWithEnablingFirUseOption(testServices, module) { fir2IrConverter.transform(module, inputArtifact.k2Artifact) } ?: return null
        return IrBackendInputsFromK1AndK2(irFromClassic, irFromFir)
    }

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return module.backendKind == BackendKinds.IrBackend
    }

}