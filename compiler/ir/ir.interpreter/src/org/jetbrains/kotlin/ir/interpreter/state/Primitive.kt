/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType

internal class Primitive<T>(val value: T, val type: IrType) : State {
    override val fields: MutableList<Variable> = mutableListOf()
    override val irClass: IrClass = type.classOrNull!!.owner

    override fun getField(symbol: IrSymbol): State? = null

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction {
        val owner = expression.symbol.owner
        return if (owner.isFakeOverride) owner.getLastOverridden() else owner
    }

    override fun toString(): String {
        return "Primitive(value=$value, type=${irClass.defaultType})"
    }
}
