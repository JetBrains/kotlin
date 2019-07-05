/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ir.underlyingType
import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.*

class JvmDefaultParameterInjector(context: JvmBackendContext) :
    DefaultParameterInjector(context, skipInline = false, skipExternalMethods = false) {

    override val context: JvmBackendContext get() = super.context as JvmBackendContext

    override fun nullConst(expression: IrElement, type: IrType): IrExpression {
        if (type !is IrSimpleType ||
            type.hasQuestionMark ||
            type.classOrNull?.owner?.isInline != true
        ) {
            return super.nullConst(expression, type)
        }
        val underlyingType = type.classOrNull!!.owner.underlyingType()
        return IrCallImpl(
            expression.startOffset, expression.endOffset,
            type,
            context.ir.symbols.unsafeCoerceIntrinsicSymbol, context.ir.symbols.unsafeCoerceIntrinsicSymbol.descriptor,
            typeArgumentsCount = 2,
            valueArgumentsCount = 1
        ).apply {
            putTypeArgument(0, underlyingType) // from
            putTypeArgument(1, type) // to
            putValueArgument(0, super.nullConst(expression, underlyingType))
        }
    }

}