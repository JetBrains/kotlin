/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.fqName
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.stack.Field
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.util.resolveFakeOverride

internal interface Complex : State {
    var superWrapperClass: Wrapper?
    var outerClass: Field?

    fun irClassFqName() = irClass.fqName

    private fun getIrFunctionFromGivenClass(irClass: IrClass, owner: IrFunction): IrFunction? {
        val propertyGetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.getter }
        val propertySetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.setter }
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        return (propertyGetters + propertySetters + functions).firstOrNull {
            when {
                it is IrSimpleFunction && owner is IrSimpleFunction -> it.overrides(owner) || owner.overrides(it)
                else -> it == owner
            }
        }
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        val receiver = expression.superQualifierSymbol?.owner ?: irClass
        val irFunction = getIrFunctionFromGivenClass(receiver, expression.symbol.owner) ?: return null
        return (irFunction as IrSimpleFunction).resolveFakeOverride()
    }

    fun loadOuterClassesInto(callStack: CallStack, receiver: IrValueSymbol? = null) {
        fun <T> List<T>.takeFromEndWhile(predicate: (T) -> Boolean): List<T> {
            val list = mutableListOf<T>()
            for (i in this.lastIndex downTo 0) {
                if (!predicate(this[i]))
                    break
                list.add(this[i])
            }
            return list
        }

        generateSequence(outerClass) { (it.second as? Complex)?.outerClass }
            .toList()
            .takeFromEndWhile { receiver == null || it.first != receiver } // only state's below receiver must be loaded on stack
            .forEach { (symbol, state) ->
                callStack.storeState(symbol, state)
                (state as? StateWithClosure)?.let { callStack.loadUpValues(it) }
            }
    }
}
