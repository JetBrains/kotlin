/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.StateWithClosure
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.name.Name
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

internal class KFunctionState(val irFunction: IrFunction, override val irClass: IrClass) : ReflectionState(), StateWithClosure {
    override val upValues: MutableList<Variable> = mutableListOf()
    override val fields: MutableList<Variable> = mutableListOf()
    private var _parameters: List<KParameter>? = null
    private var _returnType: KType? = null
    private var _typeParameters: List<KTypeParameter>? = null

    constructor(functionReference: IrFunctionReference) : this(functionReference.symbol.owner, functionReference.type.classOrNull!!.owner)
    constructor(irFunction: IrFunction, functionFactory: IrAbstractFunctionFactory) :
            this(irFunction, functionFactory.kFunctionN(irFunction.valueParameters.size))

    fun getParameters(callInterceptor: CallInterceptor): List<KParameter> {
        if (_parameters != null) return _parameters!!
        val kParameterIrClass = irClass.getIrClassOfReflectionFromList("parameters")
        var index = 0
        val instanceParameter = irFunction.dispatchReceiverParameter
            ?.let { KParameterProxy(KParameterState(kParameterIrClass, it, index++, KParameter.Kind.INSTANCE), callInterceptor) }
        val extensionParameter = irFunction.extensionReceiverParameter
            ?.let { KParameterProxy(KParameterState(kParameterIrClass, it, index++, KParameter.Kind.EXTENSION_RECEIVER), callInterceptor) }
        _parameters = listOfNotNull(instanceParameter, extensionParameter) +
                irFunction.valueParameters.map { KParameterProxy(KParameterState(kParameterIrClass, it, index++), callInterceptor) }
        return _parameters!!
    }

    fun getReturnType(callInterceptor: CallInterceptor): KType {
        if (_returnType != null) return _returnType!!
        val kTypeIrClass = irClass.getIrClassOfReflection("returnType")
        _returnType = KTypeProxy(KTypeState(irFunction.returnType, kTypeIrClass), callInterceptor)
        return _returnType!!
    }

    fun getTypeParameters(callInterceptor: CallInterceptor): List<KTypeParameter> {
        if (_typeParameters != null) return _typeParameters!!
        val kTypeParametersIrClass = irClass.getIrClassOfReflectionFromList("typeParameters")
        _typeParameters = irClass.typeParameters.map { KTypeParameterProxy(KTypeParameterState(it, kTypeParametersIrClass), callInterceptor) }
        return _typeParameters!!
    }

    fun getArity(): Int? {
        return irClass.name.asString()
            .removePrefix("Suspend").removePrefix("Function").removePrefix("KFunction")
            .toIntOrNull()
    }

    private fun isLambda(): Boolean = irFunction.name.let { it == Name.special("<anonymous>") || it == Name.special("<no name provided>") }

    override fun toString(): String {
        return if (isLambda()) renderLambda(irFunction) else renderFunction(irFunction)
    }
}