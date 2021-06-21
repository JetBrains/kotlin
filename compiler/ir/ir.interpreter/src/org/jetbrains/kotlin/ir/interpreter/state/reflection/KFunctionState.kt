/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.TEMP_FUNCTION_FOR_INTERPRETER
import org.jetbrains.kotlin.ir.interpreter.createTempClass
import org.jetbrains.kotlin.ir.interpreter.createTempFunction
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.StateWithClosure
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

internal class KFunctionState(
    val irFunction: IrFunction, override val irClass: IrClass, override val fields: MutableList<Variable>,
) : ReflectionState(), StateWithClosure {
    override val upValues: MutableList<Variable> = mutableListOf()
    private var _parameters: List<KParameter>? = null
    private var _returnType: KType? = null
    private var _typeParameters: List<KTypeParameter>? = null

    private val functionClass: IrClass
    val invokeSymbol: IrFunctionSymbol

    init {
        val invokeFunction = irClass.declarations.filterIsInstance<IrSimpleFunction>().single { it.name == OperatorNameConventions.INVOKE }
        // TODO do we need new class here? if yes, do we need different names for temp classes?
        functionClass = createTempClass(Name.identifier("Function\$0")).apply { parent = irFunction.parent }

        functionClass.superTypes += irClass.defaultType
        functionClass.declarations += createTempFunction(
            OperatorNameConventions.INVOKE, irFunction.returnType, TEMP_FUNCTION_FOR_INTERPRETER
        ).apply impl@{
            parent = functionClass
            overriddenSymbols = listOf(invokeFunction.symbol)

            dispatchReceiverParameter = invokeFunction.dispatchReceiverParameter?.deepCopyWithSymbols(initialParent = this)
            valueParameters = mutableListOf()

            val call = when (irFunction) {
                is IrSimpleFunction -> irFunction.createCall()
                is IrConstructor -> irFunction.createConstructorCall()
                else -> TODO("Unsupported symbol $symbol for invoke")
            }.apply {
                val dispatchParameter = irFunction.dispatchReceiverParameter
                val extensionParameter = irFunction.extensionReceiverParameter

                if (dispatchParameter != null) {
                    dispatchReceiver = dispatchParameter.createGetValue()
                    if (getField(dispatchParameter.symbol) == null) (this@impl.valueParameters as MutableList) += dispatchParameter
                }
                if (extensionParameter != null) {
                    extensionReceiver = extensionParameter.createGetValue()
                    if (getField(extensionParameter.symbol) == null) (this@impl.valueParameters as MutableList) += extensionParameter
                }
                irFunction.valueParameters.forEach {
                    putArgument(it, it.createGetValue())
                    (this@impl.valueParameters as MutableList) += it
                }
            }

            body = listOf(this.createReturn(call)).wrapWithBlockBody()
            invokeSymbol = this.symbol
        }
    }

    constructor(irFunction: IrFunction, irClass: IrClass) : this(irFunction, irClass, mutableListOf())

    constructor(functionReference: IrFunctionReference, dispatchReceiver: Variable?, extensionReceiver: Variable?) : this(
        functionReference.symbol.owner,
        functionReference.type.classOrNull!!.owner,
        listOfNotNull(dispatchReceiver, extensionReceiver).toMutableList()
    ) {
        // receivers are used in comparison of two functions in KFunctionProxy
        upValues += fields
    }

    constructor(irFunction: IrFunction, irBuiltIns: IrBuiltIns) :
            this(irFunction, irBuiltIns.kFunctionN(irFunction.valueParameters.size), mutableListOf())

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        if (expression.symbol.owner.name == OperatorNameConventions.INVOKE) return invokeSymbol.owner
        return super.getIrFunctionByIrCall(expression)
    }

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