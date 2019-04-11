/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

internal val removeDeclarationsThatWouldBeInlined = makeIrFilePhase(
    ::RemoveDeclarationsThatWouldBeInlinedLowering,
    name = "RemoveInlinedDeclarations",
    description = "Rename declaration that should be inlined"
)

private class RemoveDeclarationsThatWouldBeInlinedLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val loweredLambdasToDelete = hashSetOf<IrDeclaration>()
        irFile.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val owner = expression.symbol.owner
                if (expression.symbol.owner.isInlineFunctionCall(context)) {
                    owner.valueParameters.filter {
                        !it.isNoinline && it.type.isFunction() && !it.type.isNullable()
                    }.forEach {
                        val valueArgument = expression.getValueArgument(it.index) as? IrContainerExpression ?: return@forEach
                        if (isInlineIrExpression(valueArgument)) {
                            val reference =
                                valueArgument.statements.firstIsInstanceOrNull<IrFunctionReference>() ?: return@forEach
                            loweredLambdasToDelete.add(reference.symbol.owner)
                        }
                    }

                }
                return super.visitCall(expression)
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
