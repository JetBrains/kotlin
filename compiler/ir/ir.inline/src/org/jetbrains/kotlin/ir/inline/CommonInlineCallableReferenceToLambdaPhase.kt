/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.inline

import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.util.isInlineArrayConstructor
import org.jetbrains.kotlin.ir.util.isInlineParameter

/**
 * Transforms all callable references (including defaults) to inline lambdas, marks inline lambdas for later passes.
 */
open class CommonInlineCallableReferenceToLambdaPhase(
    context: LoweringContext,
    inlineFunctionResolver: InlineFunctionResolver
) : InlineCallableReferenceToLambdaPhase(context, inlineFunctionResolver) {
    fun lower(function: IrFunction) = function.accept(this, function.parent)

    override fun visitFunction(declaration: IrFunction, data: IrDeclarationParent?): IrStatement {
        super.visitFunction(declaration, data)
        if (inlineFunctionResolver.needsInlining(declaration)) {
            for (parameter in declaration.parameters) {
                if (parameter.isInlineParameter()) {
                    val defaultExpression = parameter.defaultValue?.expression ?: continue
                    parameter.defaultValue?.expression = defaultExpression.transformToLambda(declaration)
                }
            }
        }

        return declaration
    }

    override fun visitFunctionReference(expression: IrFunctionReference, data: IrDeclarationParent?): IrElement {
        super.visitFunctionReference(expression, data)

        val owner = expression.symbol.owner
        if (!owner.isInlineArrayConstructor()) return expression

        return expression.transformToLambda(data)
    }
}
