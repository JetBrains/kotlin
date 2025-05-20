/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType

interface CallInlinerStrategy {
    /**
     * TypeOf function requires some custom backend-specific processing. This is a customization point for that.
     *
     * @param expression is a copy of original IrCall with types substituted by normal rules
     * @param nonSubstitutedTypeArgument is typeArgument of call with only reified type parameters substituted
     *
     * @return new node to insert instead of typeOf call.
     */
    fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression
    fun at(container: IrDeclaration, expression: IrExpression) {}

    object DEFAULT : CallInlinerStrategy {
        override fun postProcessTypeOf(expression: IrCall, nonSubstitutedTypeArgument: IrType): IrExpression {
            return expression.apply {
                typeArguments[0] = nonSubstitutedTypeArgument
            }
        }
    }
}