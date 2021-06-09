/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.*

internal interface Complex: State {
    var superWrapperClass: Wrapper?
    var outerClass: Variable?

    fun irClassFqName(): String {
        return irClass.fqNameForIrSerialization.toString()
    }

    private fun getIrFunctionFromGivenClass(irClass: IrClass, symbol: IrFunctionSymbol): IrFunction? {
        val propertyGetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.getter }
        val propertySetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.setter }
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        return (propertyGetters + propertySetters + functions).firstOrNull {
            val owner = symbol.owner
            when {
                it is IrSimpleFunction && owner is IrSimpleFunction -> it.overrides(owner) || owner.overrides(it)
                else -> it == symbol.owner
            }
        }
    }

    fun getOverridden(owner: IrSimpleFunction): IrSimpleFunction {
        if (owner.parent == superWrapperClass?.irClass) return owner
        if (!owner.isFakeOverride || owner.body != null || owner.parentAsClass.defaultType.isAny()) return owner

        val overriddenOwner = owner.overriddenSymbols.let { it.singleOrNull { it.owner.body != null } ?: it.singleOrNull() }?.owner
        return overriddenOwner?.let { getOverridden(it) } ?: owner.getLastOverridden() as IrSimpleFunction
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        val receiver = expression.superQualifierSymbol?.owner ?: irClass
        val irFunction = getIrFunctionFromGivenClass(receiver, expression.symbol) ?: return null
        return getOverridden(irFunction as IrSimpleFunction)
    }

    fun loadOuterClassesInto(callStack: CallStack) {
        generateSequence(outerClass) { (it.state as? Complex)?.outerClass }.forEach { callStack.addVariable(it) }
    }
}