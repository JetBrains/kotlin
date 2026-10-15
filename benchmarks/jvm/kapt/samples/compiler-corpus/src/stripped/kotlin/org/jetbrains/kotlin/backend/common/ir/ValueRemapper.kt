/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.ir

import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.isAssignable
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

abstract class AbstractValueRemapper : IrElementTransformerVoid() {

    protected abstract fun remapValue(oldValue: IrValueSymbol): IrValueSymbol?

    override fun visitGetValue(expression: IrGetValue): IrExpression { TODO() }

    override fun visitSetValue(expression: IrSetValue): IrExpression { TODO() }
}

open class ValueRemapper(protected open val map: Map<out IrValueSymbol, IrValueSymbol>) : AbstractValueRemapper() {
    override fun remapValue(oldValue: IrValueSymbol): IrValueSymbol? = TODO()
}