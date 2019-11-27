/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isLambda
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.visitors.*

internal val removeDeclarationsThatWouldBeInlined = makeIrModulePhase(
    ::RemoveDeclarationsThatWouldBeInlinedLowering,
    name = "RemoveInlinedDeclarations",
    description = "Rename declaration that should be inlined"
)

// Removes all functions which are only used as arguments to inline functions. It's
// important that this phase runs right before codegen, since we need the bodies of lambdas to
// be lowered for inline codegen. Conversely, since this phase runs right before codegen we can
// assume that all remaining function references are only used as arguments to inline functions -
// otherwise they would have been lowered.
private class RemoveDeclarationsThatWouldBeInlinedLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val loweredLambdasToDelete = mutableSetOf<IrFunction>()

        irFile.acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

            override fun visitFunctionReference(expression: IrFunctionReference) {
                if (expression.origin.isLambda) {
                    loweredLambdasToDelete.add(expression.symbol.owner)
                }

                expression.acceptChildrenVoid(this)
            }
        })

        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                return super.visitClass(declaration).also {
                    declaration.declarations.removeAll(loweredLambdasToDelete)
                }
            }
        })
    }
}
