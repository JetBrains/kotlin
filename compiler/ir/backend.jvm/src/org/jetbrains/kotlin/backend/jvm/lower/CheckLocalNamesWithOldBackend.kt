/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

val checkLocalNamesWithOldBackendPhase = makeIrFilePhase<JvmBackendContext>(
    { context ->
        if (context.state.languageVersionSettings.getFlag(JvmAnalysisFlags.irCheckLocalNames))
            CheckLocalNamesWithOldBackend(context)
        else
            FileLoweringPass.Empty
    },
    name = "CheckLocalNamesWithOldBackend",
    description = "With -Xir-check-local-names, check that names for local classes and anonymous objects are the same in the IR backend as in the old backend"
)

class CheckLocalNamesWithOldBackend(private val context: JvmBackendContext) : FileLoweringPass, IrElementVisitorVoid {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(this)
    }

    override fun visitClass(declaration: IrClass) {
        val actualName = context.getLocalClassType(declaration)?.internalName
        if (actualName != null) {
            val expectedName = context.state.bindingTrace.get(CodegenBinding.ASM_TYPE, declaration.symbol.descriptor)?.internalName
            if (expectedName != null && expectedName != actualName) {
                throw AssertionError(
                    "Incorrect name for the class.\n" +
                            "IR: ${declaration.render()}\n" +
                            "Descriptor: ${declaration.descriptor}\n" +
                            "Expected name: $expectedName\n" +
                            "Actual name: $actualName"
                )
            }
        }
        super.visitClass(declaration)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }
}
