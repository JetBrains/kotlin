/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

internal val prepareForBytecodeInlining = makeIrModulePhase(
    ::BytecodeInliningPreparationLowering,
    name = "BytecodeInliningPreparation",
    description = "Label all loops for non-local break/continue"
)

private class BytecodeInliningPreparationLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.accept(object : IrElementVisitor<Unit, String> {
            // This counter is intentionally not local to every declaration because their names might clash.
            private var counter = 0

            override fun visitElement(element: IrElement, data: String) =
                element.acceptChildren(this, if (element is IrDeclarationWithName) "$data${element.name}$" else data)

            override fun visitLoop(loop: IrLoop, data: String) {
                // Give all loops unique labels so that we can generate unambiguous instructions for non-local
                // `break`/`continue` statements.
                loop.label = "$data${++counter}"
                super.visitLoop(loop, data)
            }
        }, "")
    }
}
