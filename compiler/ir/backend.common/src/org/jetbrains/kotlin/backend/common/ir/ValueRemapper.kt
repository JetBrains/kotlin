/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

abstract class AbstractValueRemapper : IrElementTransformerVoid() {

    protected abstract fun remapValue(oldValue: IrValueSymbol): IrValueSymbol?

    override fun visitGetValue(expression: IrGetValue): IrExpression {
        val newValue = remapValue(expression.symbol) ?: return expression
        return expression.run { IrGetValueImpl(startOffset, endOffset, newValue, origin) }
    }

    override fun visitSetValue(expression: IrSetValue): IrExpression {
        expression.transformChildrenVoid()
        val newValue = remapValue(expression.symbol) ?: return expression
        assert(newValue.owner.isAssignable)
        return expression.run { IrSetValueImpl(startOffset, endOffset, type, newValue, value, origin) }
    }
}

open class ValueRemapper(protected open val map: Map<IrValueSymbol, IrValueSymbol>) : AbstractValueRemapper() {
    override fun remapValue(oldValue: IrValueSymbol): IrValueSymbol? = map[oldValue]
}