/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.anyOfOverriddenFunctionsReturnsNonUnit
import org.jetbrains.kotlin.backend.jvm.codegen.isKnownToBeTailCall
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal val tailCallOptimizationPhase = makeIrFilePhase(
    ::TailCallOptimizationLowering,
    "TailCallOptimization",
    "Add or move returns to suspension points on tail-call positions"
)

// TODO: Make this lowering common
private class TailCallOptimizationLowering(private val context: JvmBackendContext) : IrElementVisitorVoid, FileLoweringPass {
    override fun lower(irFile: IrFile) {
        visitFile(irFile)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(function: IrFunction) {
        if (!function.isSuspend || function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA || function.isKnownToBeTailCall()) return
        // Disable tail-call optimization for functions, returning Unit and overriding function, returning non-unit.
        if (function.returnType.isUnit() && function.anyOfOverriddenFunctionsReturnsNonUnit()) return

        val tailCalls = mutableSetOf<IrCall>()

        // Find all tail-calls inside suspend function.
        // We should add IrReturn before them, so the codegen will generate code, which is understandable by old BE's tail-call optimizer.
        // This function collect all possible tail-calls, if they are not prepended by IrReturns, so we can prepend them.
        // If the call has IrReturn before it, we do not need to add another one.
        // If the call is inside a branch of `when` statement (or `if`, which is lowered to `when`), go through the branches and
        // find tail-calls there.
        fun findCallsOnTailPositionWithoutImmediateReturn(statement: IrStatement, immediateReturn: Boolean = false) {
            when (statement) {
                is IrCall -> if (statement.isSuspend && !immediateReturn) {
                    tailCalls += statement
                }
                is IrBlock -> findCallsOnTailPositionWithoutImmediateReturn(
                    statement.statements.findTailCall(function.returnType.isUnit()) ?: return
                )
                is IrWhen -> for (branch in statement.branches) {
                    findCallsOnTailPositionWithoutImmediateReturn(branch.result)
                }
                is IrReturn -> findCallsOnTailPositionWithoutImmediateReturn(statement.value, immediateReturn = true)
                is IrTypeOperatorCall -> if (statement.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
                    findCallsOnTailPositionWithoutImmediateReturn(statement.argument)
                }
                else -> {
                    // Not a call, or something containing a tail-call, break
                    // TODO: Support binary logical operations and elvis, though. KT-23826 and KT-23825
                }
            }
        }

        when (val body = function.body) {
            null -> return
            is IrBlockBody -> findCallsOnTailPositionWithoutImmediateReturn(
                body.statements.findTailCall(function.returnType.isUnit()) ?: return
            )
            is IrExpressionBody -> findCallsOnTailPositionWithoutImmediateReturn(body.expression)
            else -> error("Unexpected $body")
        }

        function.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(call: IrCall): IrExpression {
                if (call !in tailCalls) return call
                if (!function.returnType.isUnit() && call.type != function.returnType) return call
                // Replace ARETURN with { POP, GETSTATIC kotlin/Unit.INSTANCE, ARETURN } during codegen later.
                // Otherwise, additional CHECKCAST will break tail-call optimization
                if (function.returnType.isUnit()) {
                    context.suspendTailCallsWithUnitReplacement += call.attributeOwnerId
                }
                return IrReturnImpl(call.startOffset, call.endOffset, context.irBuiltIns.nothingType, function.symbol, call)
            }
        })
    }
}

// Find tail-call inside a single block. This function is needed, since there can be
// return statement in the middle of the function and thus we cannot just assume, that its last statement is tail-call
private fun List<IrStatement>.findTailCall(functionReturnsUnit: Boolean): IrStatement? {
    val mayBeReturn = find { it is IrReturn } as? IrReturn
    return when (val value = mayBeReturn?.value) {
        is IrGetField -> if (functionReturnsUnit && value.isGetFieldOfUnit()) {
            // This is simple `return` in the middle of a function
            // Tail-call should be just before it
            subList(0, indexOf(mayBeReturn)).findTailCall(functionReturnsUnit)
        } else mayBeReturn
        null -> lastOrNull()
        else -> mayBeReturn
    }
}

private fun IrGetField.isGetFieldOfUnit(): Boolean =
    type.isUnit() && symbol.owner.name == Name.identifier("INSTANCE") && symbol.owner.parentAsClass.fqNameWhenAvailable == FqName("kotlin.Unit")
