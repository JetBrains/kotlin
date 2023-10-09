/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.test.model.AbstractTestFacade
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runWithEnablingFirUseOption
import org.jetbrains.kotlin.test.services.TestServices

class K1AndK2IrActualizerAndPluginsFacade(val testServices: TestServices) :
    AbstractTestFacade<IrBackendInputsFromK1AndK2, IrBackendInputsFromK1AndK2>() {
    override val inputKind: TestArtifactKind<IrBackendInputsFromK1AndK2>
        get() = BackendKinds.IrBackendForK1AndK2
    override val outputKind: TestArtifactKind<IrBackendInputsFromK1AndK2>
        get() = BackendKinds.IrBackendForK1AndK2

    override fun shouldRunAnalysis(module: TestModule): Boolean {
        return true
    }

    private val irActualizerAndPluginsFacade = IrActualizerAndPluginsFacade(testServices)

    override fun transform(module: TestModule, inputArtifact: IrBackendInputsFromK1AndK2): IrBackendInputsFromK1AndK2? {
        val fromFir =
            runWithEnablingFirUseOption(testServices, module) { irActualizerAndPluginsFacade.transform(module, inputArtifact.fromK2) }
        return IrBackendInputsFromK1AndK2(inputArtifact.fromK1, fromFir)
    }

}