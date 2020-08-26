/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.interpreter.getCorrectReceiverByFunction
import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.name.Name

internal abstract class Complex(override val irClass: IrClass, override val fields: MutableList<Variable>) : State {
    var superClass: Complex? = null
    var subClass: Complex? = null
    val interfaces: MutableList<Complex> = mutableListOf() // filled lazily, as needed
    override val typeArguments: MutableList<Variable> = mutableListOf()
    var outerClass: Variable? = null

    fun setSuperClassInstance(superClass: Complex) {
        if (this.irClass == superClass.irClass) {
            // if superClass is just secondary constructor instance, then copy properties that isn't already present in instance
            superClass.fields.forEach { if (!this.contains(it)) fields.add(it) }
            this.superClass = superClass.superClass
            superClass.superClass?.subClass = this
        } else {
            this.superClass = superClass
            superClass.subClass = this
        }
    }

    fun getOriginal(): Complex {
        return subClass?.getOriginal() ?: this
    }

    fun irClassFqName(): String {
        return irClass.fqNameForIrSerialization.toString()
    }

    private fun contains(variable: Variable) = fields.any { it.symbol == variable.symbol }

    private fun getIrFunction(symbol: IrFunctionSymbol): IrFunction? {
        val propertyGetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.getter }
        val propertySetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.setter }
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        return (propertyGetters + propertySetters + functions).firstOrNull {
            if (it is IrSimpleFunction) it.overrides(symbol.owner as IrSimpleFunction) else it == symbol.owner
        }
    }

    private fun getThisOrSuperReceiver(superIrClass: IrClass?): Complex? {
        return when {
            superIrClass == null -> this.getOriginal()
            superIrClass.isInterface -> Common(superIrClass).apply {
                interfaces.add(this)
                this.subClass = this@Complex
            }
            else -> this.superClass
        }
    }

    protected fun getOverridden(owner: IrSimpleFunction, qualifier: State?): IrSimpleFunction {
        if (!owner.isFakeOverride) return owner
        if (qualifier == null || qualifier is ExceptionState || (qualifier as? Complex)?.superClass == null) {
            return owner.getLastOverridden() as IrSimpleFunction
        }

        val overriddenOwner = owner.overriddenSymbols.single().owner
        return when {
            overriddenOwner.body != null -> overriddenOwner
            else -> getOverridden(overriddenOwner, qualifier.superClass!!)
        }
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        val receiver = getThisOrSuperReceiver(expression.superQualifierSymbol?.owner) ?: return null

        val irFunction = receiver.getIrFunction(expression.symbol) ?: return null

        return when (irFunction.body) {
            null -> getOverridden(irFunction as IrSimpleFunction, this.getCorrectReceiverByFunction(irFunction))
            else -> irFunction
        }
    }

    fun getEqualsFunction(): IrSimpleFunction {
        val equalsFun = irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .single {
                it.name == Name.identifier("equals") && it.dispatchReceiverParameter != null
                        && it.valueParameters.size == 1 && it.valueParameters[0].type.isNullableAny()
            }
        return getOverridden(equalsFun, this)
    }

    fun getHashCodeFunction(): IrSimpleFunction {
        return irClass.declarations.filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() == "hashCode" }
            .first { it.valueParameters.isEmpty() }
            .let { getOverridden(it, this) }
    }

    fun getToStringFunction(): IrSimpleFunction {
        return irClass.declarations.filterIsInstance<IrSimpleFunction>()
            .filter { it.name.asString() == "toString" }
            .first { it.valueParameters.isEmpty() }
            .let { getOverridden(it, this) }
    }
}