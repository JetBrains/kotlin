/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.interpreter.isFunction
import org.jetbrains.kotlin.ir.interpreter.isKFunction
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal abstract class ReflectionState(override val irClass: IrClass) : State {
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? = null
}

internal class KPropertyState(
    val propertyReference: IrPropertyReference, val dispatchReceiver: State?, val extensionReceiver: State?
) : ReflectionState(propertyReference.symbol.owner.parentAsClass) {
    val className = propertyReference.type.classOrNull?.owner?.name?.asString()

    fun isKProperty0(): Boolean {
        return className == "KProperty0"
    }

    fun isKProperty1(): Boolean {
        return className == "KProperty1"
    }

    fun isKProperty2(): Boolean {
        return className == "KProperty2"
    }

    fun isKMutableProperty0(): Boolean {
        return className == "KMutableProperty0"
    }

    fun isKMutableProperty1(): Boolean {
        return className == "KMutableProperty1"
    }

    fun isKMutableProperty2(): Boolean {
        return className == "KMutableProperty2"
    }
}

internal class KFunctionState(val irFunction: IrFunction, override val irClass: IrClass) : ReflectionState(irClass) {
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()
    val isKFunction = irClass.defaultType.isKFunction()
    val isFunction = irClass.defaultType.isFunction()

    constructor(functionReference: IrFunctionReference) : this(functionReference.symbol.owner, functionReference.type.classOrNull!!.owner)

    private val invokeSymbol = irClass.declarations
        .single { it.nameForIrSerialization.asString() == "invoke" }
        .cast<IrSimpleFunction>()
        .getLastOverridden().symbol

    fun getArity(): Int? {
        return irClass.name.asString().removePrefix("Function").removePrefix("KFunction").toIntOrNull()
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        return if (invokeSymbol == expression.symbol) irFunction else null
    }

    override fun toString(): String {
        val receiver = (irFunction.dispatchReceiverParameter?.type ?: irFunction.extensionReceiverParameter?.type)?.render()
        val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.render() }
        val returnType = irFunction.returnType.render()
        return ("$arguments -> $returnType").let { if (receiver != null) "$receiver.$it" else it }
    }
}