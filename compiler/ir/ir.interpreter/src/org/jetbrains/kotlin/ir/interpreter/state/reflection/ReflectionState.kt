/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.renderType
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

internal abstract class ReflectionState(val irClassifierSymbol: IrClassifierSymbol) : State {
    override val irClass: IrClass = irClassifierSymbol.extractClass()
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()

    constructor(irTypeParameter: IrTypeParameter) : this(irTypeParameter.superTypes.firstNotNullResult { it.classifierOrNull }!!)

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? = null

    protected fun renderReceivers(dispatchReceiver: IrType?, extensionReceiver: IrType?): String {
        return buildString {
            if (dispatchReceiver != null) {
                append(dispatchReceiver.renderType())
                append(".")
            }
            val addParentheses = dispatchReceiver != null && extensionReceiver != null
            if (addParentheses) append("(")
            if (extensionReceiver != null) {
                append(extensionReceiver.renderType())
                append(".")
            }
            if (addParentheses) append(")")
        }
    }

    companion object {
        private fun IrClassifierSymbol.extractClass(): IrClass {
            return (owner as? IrClass) ?: (owner as IrTypeParameter).extractAnyClass()
        }

        private fun IrTypeParameter.extractAnyClass(): IrClass {
            return this.superTypes
                .firstNotNullResult { it.classOrNull?.owner ?: (it.classifierOrNull?.owner as? IrTypeParameter)?.extractAnyClass() }!!
        }
    }
}
