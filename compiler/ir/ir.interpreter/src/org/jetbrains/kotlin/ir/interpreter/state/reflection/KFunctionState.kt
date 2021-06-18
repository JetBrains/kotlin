/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.StateWithClosure
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
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
        functionClass = IrFactoryImpl.createClass(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            object : IrDeclarationOriginImpl("TEMP_CLASS_FOR_INTERPRETER") {}, IrClassSymbolImpl(),
            Name.identifier("Function\$0"), ClassKind.CLASS, DescriptorVisibilities.PRIVATE, Modality.FINAL
        ).apply {
            parent = irFunction.parent
        }

        functionClass.superTypes += irClass.defaultType
        functionClass.declarations += IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            object : IrDeclarationOriginImpl("TEMP_FUNCTION_FOR_INTERPRETER") {}, IrSimpleFunctionSymbolImpl(),
            OperatorNameConventions.INVOKE, DescriptorVisibilities.PUBLIC, Modality.FINAL, irFunction.returnType,
            isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isOperator = true, isInfix = false, isExpect = false
        ).apply impl@{
            parent = functionClass
            overriddenSymbols = listOf(invokeFunction.symbol)

            dispatchReceiverParameter = invokeFunction.dispatchReceiverParameter?.deepCopyWithSymbols(initialParent = this)
            valueParameters = mutableListOf()

            val call = when (val symbol = irFunction.symbol) {
                is IrSimpleFunctionSymbol -> IrCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    irFunction.returnType, symbol, irFunction.typeParameters.size, irFunction.valueParameters.size
                )
                is IrConstructorSymbol -> IrConstructorCallImpl.fromSymbolOwner(irFunction.returnType, symbol)
                else -> TODO("Unsupported symbol $symbol for invoke")
            }.apply {
                val dispatchParameter = irFunction.dispatchReceiverParameter
                val extensionParameter = irFunction.extensionReceiverParameter

                if (dispatchParameter != null) {
                    dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchParameter.type, dispatchParameter.symbol)
                    if (getField(dispatchParameter.symbol) == null) (this@impl.valueParameters as MutableList).add(dispatchParameter)
                }
                if (extensionParameter != null) {
                    extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, extensionParameter.type, extensionParameter.symbol)
                    if (getField(extensionParameter.symbol) == null) (this@impl.valueParameters as MutableList).add(extensionParameter)
                }
                irFunction.valueParameters.forEach {
                    putArgument(it, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it.type, it.symbol))
                    (this@impl.valueParameters as MutableList).add(it)
                }
            }

            body = IrBlockBodyImpl(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                listOf(IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, this.returnType, this.symbol, call))
            )

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