/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.Name

internal val uniqueLoopLabelsPhase = makeIrFilePhase(
    { _: JvmBackendContext -> UniqueLoopLabelsLowering() },
    name = "UniqueLoopLabels",
    description = "Label all loops for non-local break/continue"
)

private class UniqueLoopLabelsLowering : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        irFile.acceptVoid(object : IrElementVisitorVoid {
            // This counter is intentionally not local to every declaration because their names might clash.
            private var counter = 0
            private val stack = ArrayList<Name>()

            override fun visitElement(element: IrElement) {
                if (element is IrDeclarationWithName) {
                    stack.add(element.name)
                    element.acceptChildrenVoid(this)
                    stack.removeLast()
                } else {
                    element.acceptChildrenVoid(this)
                }
            }

            override fun visitLoop(loop: IrLoop) {
                // Give all loops unique labels so that we can generate unambiguous instructions for non-local
                // `break`/`continue` statements.
                loop.label = stack.joinToString("$", postfix = (++counter).toString())
                super.visitLoop(loop)
            }
        })
    }
}
