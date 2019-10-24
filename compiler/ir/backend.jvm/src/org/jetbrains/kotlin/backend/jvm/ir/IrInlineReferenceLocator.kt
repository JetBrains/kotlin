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
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class IrInlineReferenceLocator(private val context: JvmBackendContext) : IrElementVisitorVoidWithContext() {
    val inlineReferences = mutableSetOf<IrCallableReference>()

    // For crossinline lambdas, the call site is null as it's probably in a separate class somewhere.
    // All other lambdas are guaranteed to be inlined into the scope they are declared in.
    val lambdaToCallSite = mutableMapOf<IrFunction, IrDeclaration?>()

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

                if (valueArgument is IrPropertyReference) {
                    inlineReferences.add(valueArgument)
                    continue
                }

                val reference = when (valueArgument) {
                    is IrFunctionReference -> valueArgument
                    is IrBlock -> valueArgument.statements.filterIsInstance<IrFunctionReference>().singleOrNull()
                    else -> null
                } ?: continue

                inlineReferences.add(reference)
                if (valueArgument is IrBlock && valueArgument.origin.isLambda) {
                    lambdaToCallSite[reference.symbol.owner] =
                        if (parameter.isCrossinline) null else currentScope!!.irElement as IrDeclaration
                }
            }
        }
        return super.visitFunctionAccess(expression)
    }

    companion object {
        fun scan(context: JvmBackendContext, element: IrElement) =
            IrInlineReferenceLocator(context).apply { element.accept(this, null) }
    }
}