/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.getLastOverridden
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.renderType
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast
import kotlin.reflect.KTypeParameter

internal class KFunctionState(val irFunction: IrFunction, override val irClass: IrClass) : ReflectionState(irClass.symbol) {
    override val fields: MutableList<Variable> = mutableListOf()
    override val typeArguments: MutableList<Variable> = mutableListOf()
    private var _typeParameters: List<KTypeParameter>? = null

    constructor(functionReference: IrFunctionReference) : this(functionReference.symbol.owner, functionReference.type.classOrNull!!.owner)
    constructor(irFunction: IrFunction, functionFactory: IrAbstractFunctionFactory) :
            this(irFunction, functionFactory.kFunctionN(irFunction.valueParameters.size))

    fun getTypeParameters(interpreter: IrInterpreter): List<KTypeParameter> {
        if (_typeParameters != null) return _typeParameters!!
        _typeParameters = irClass.typeParameters.map { KTypeParameterProxy(KTypeParameterState(it), interpreter) }
        return _typeParameters!!
    }

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

    private fun isLambda(): Boolean = irFunction.name.let { it == Name.special("<anonymous>") || it == Name.special("<no name provided>") }

    override fun toString(): String {
        return if (isLambda()) {
            val receiver = (irFunction.dispatchReceiverParameter?.type ?: irFunction.extensionReceiverParameter?.type)?.renderType()
            val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.renderType() }
            val returnType = irFunction.returnType.renderType()
            ("$arguments -> $returnType").let { if (receiver != null) "$receiver.$it" else it }
        } else {
            val dispatchReceiver = irFunction.parentAsClass.defaultType // = instanceReceiverParameter
            val extensionReceiver = irFunction.extensionReceiverParameter?.type
            val receivers = if (irFunction is IrConstructor) "" else renderReceivers(dispatchReceiver, extensionReceiver)
            val arguments = irFunction.valueParameters.joinToString(prefix = "(", postfix = ")") { it.type.renderType() }
            val returnType = irFunction.returnType.renderType()
            "fun $receivers${irFunction.name}$arguments: $returnType"
        }
    }
}