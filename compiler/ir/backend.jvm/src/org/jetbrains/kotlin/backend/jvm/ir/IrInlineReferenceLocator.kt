/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.unwrapInlineLambda
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrInlineReferenceLocator(private val context: JvmBackendContext) : IrElementVisitor<Unit, IrDeclaration?> {
    override fun visitElement(element: IrElement, data: IrDeclaration?) =
        element.acceptChildren(this, if (element is IrDeclaration && element !is IrVariable) element else data)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression, data: IrDeclaration?) {
        val function = expression.symbol.owner
        if (function.isInlineFunctionCall(context)) {
            for (parameter in function.valueParameters) {
                val lambda = expression.getValueArgument(parameter.index)?.unwrapInlineLambda() ?: continue
                visitInlineLambda(lambda, function, parameter, data!!)
            }
        }
        return super.visitFunctionAccess(expression, data)
    }

    abstract fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration)
}

inline fun IrFile.findInlineLambdas(
    context: JvmBackendContext, crossinline onLambda: (IrFunctionReference, IrFunction, IrValueParameter, IrDeclaration) -> Unit
) = accept(object : IrInlineReferenceLocator(context) {
    override fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) =
        onLambda(argument, callee, parameter, scope)
}, null)
