/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.ir

import org.jetbrains.kotlin.backend.common.IrElementVisitorVoidWithContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.codegen.isInlineIrExpression
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal open class IrInlineReferenceLocator(private val context: JvmBackendContext) : IrElementVisitorVoidWithContext() {
    override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

    override fun visitFunctionAccess(expression: IrFunctionAccessExpression) {
        val function = expression.symbol.owner
        if (function.isInlineFunctionCall(context)) {
            for (parameter in function.valueParameters) {
                if (!parameter.isInlineParameter())
                    continue

                val valueArgument = expression.getValueArgument(parameter.index) ?: continue
                if (!isInlineIrExpression(valueArgument))
                    continue

                if (valueArgument is IrBlock && valueArgument.origin.isLambda) {
                    val reference = valueArgument.statements.last() as IrFunctionReference
                    visitInlineLambda(reference, function, parameter, currentScope!!.irElement as IrDeclaration)
                } else if (valueArgument is IrCallableReference) {
                    visitInlineReference(valueArgument)
                }
            }
        }
        return super.visitFunctionAccess(expression)
    }

    open fun visitInlineReference(argument: IrCallableReference) {}

    open fun visitInlineLambda(argument: IrFunctionReference, callee: IrFunction, parameter: IrValueParameter, scope: IrDeclaration) =
        visitInlineReference(argument)

    companion object {
        fun scan(context: JvmBackendContext, element: IrElement): Set<IrCallableReference> =
            mutableSetOf<IrCallableReference>().apply {
                element.accept(object : IrInlineReferenceLocator(context) {
                    override fun visitInlineReference(argument: IrCallableReference) {
                        add(argument)
                    }
                }, null)
            }
    }
}