/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.lower.DefaultParameterInjector
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.lower.inlineclasses.unboxInlineClass
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull

class JvmDefaultParameterInjector(context: JvmBackendContext) :
    DefaultParameterInjector(context, skipInline = false, skipExternalMethods = false) {

    override val context: JvmBackendContext get() = super.context as JvmBackendContext

    override fun nullConst(startOffset: Int, endOffset: Int, irParameter: IrValueParameter): IrExpression? =
        nullConst(startOffset, endOffset, irParameter.type)

    override fun nullConst(startOffset: Int, endOffset: Int, type: IrType): IrExpression {
        if (type !is IrSimpleType || type.hasQuestionMark || type.classOrNull?.owner?.isInline != true)
            return super.nullConst(startOffset, endOffset, type)

        val underlyingType = type.unboxInlineClass()
        return IrCallImpl(startOffset, endOffset, type, context.ir.symbols.unsafeCoerceIntrinsicSymbol).apply {
            putTypeArgument(0, underlyingType) // from
            putTypeArgument(1, type) // to
            putValueArgument(0, super.nullConst(startOffset, endOffset, underlyingType))
        }
    }
}
