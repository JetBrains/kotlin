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
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

internal interface Complex: State {
    var superWrapperClass: Wrapper?
    override val typeArguments: MutableList<Variable>
    var outerClass: Variable?

    fun irClassFqName(): String {
        return irClass.fqNameForIrSerialization.toString()
    }

    private fun IrClass.getIrFunction(symbol: IrFunctionSymbol): IrFunction? {
        val propertyGetters = this.declarations.filterIsInstance<IrProperty>().mapNotNull { it.getter }
        val propertySetters = this.declarations.filterIsInstance<IrProperty>().mapNotNull { it.setter }
        val functions = this.declarations.filterIsInstance<IrFunction>()
        return (propertyGetters + propertySetters + functions).firstOrNull {
            val owner = symbol.owner
            when {
                it is IrSimpleFunction && owner is IrSimpleFunction -> it.overrides(owner) || owner.overrides(it)
                else -> it == symbol.owner
            }
        }
    }

    private fun getThisOrSuperReceiver(superIrClass: IrClass?): IrClass? {
        return when {
            superIrClass == null -> this.irClass
            superIrClass.isInterface -> superIrClass
            else -> irClass.superTypes.map { it.classOrNull?.owner }.singleOrNull { it?.isInterface == false }
        }
    }

    private fun getOverridden(owner: IrSimpleFunction): IrSimpleFunction {
        if (owner.parent == superWrapperClass?.irClass) return owner
        if (!owner.isFakeOverride || owner.body != null || owner.parentAsClass.defaultType.isAny()) return owner

        val overriddenOwner = owner.overriddenSymbols.singleOrNull { !it.owner.parentAsClass.isInterface }?.owner
        return overriddenOwner?.let { getOverridden(it) } ?: owner.getLastOverridden() as IrSimpleFunction
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        val receiver = getThisOrSuperReceiver(expression.superQualifierSymbol?.owner) ?: return null
        val irFunction = receiver.getIrFunction(expression.symbol) ?: return null
        return getOverridden(irFunction as IrSimpleFunction)
    }

    fun getEqualsFunction(): IrSimpleFunction {
        val equalsFun = irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .single {
                it.name == Name.identifier("equals") && it.dispatchReceiverParameter != null
                        && it.valueParameters.size == 1 && it.valueParameters[0].type.isNullableAny()
            }
        return getOverridden(equalsFun)
    }

    fun getHashCodeFunction(): IrSimpleFunction {
        return irClass.declarations.filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() == "hashCode" }
            .first { it.valueParameters.isEmpty() }
            .let { getOverridden(it) }
    }

    fun getToStringFunction(): IrSimpleFunction {
        return irClass.declarations.filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() == "toString" }
            .first { it.valueParameters.isEmpty() }
            .let { getOverridden(it) }
    }
}