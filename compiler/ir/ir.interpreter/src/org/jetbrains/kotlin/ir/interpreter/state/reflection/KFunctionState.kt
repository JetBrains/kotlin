/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.putArgument
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.stack.Field
import org.jetbrains.kotlin.ir.interpreter.stack.Fields
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.state.StateWithClosure
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.util.OperatorNameConventions
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

internal class KFunctionState(
    val irFunction: IrFunction,
    override val irClass: IrClass,
    environment: IrInterpreterEnvironment,
    override val fields: Fields = mutableMapOf()
) : ReflectionState(), StateWithClosure {
    override val upValues: MutableMap<IrSymbol, Variable> = mutableMapOf()

    var funInterface: IrType? = null
        set(value) {
            field = value ?: return
            val samFunction = value.classOrNull!!.owner.getSingleAbstractMethod()
            if (samFunction.extensionReceiverParameter != null) {
                // this change of parameter is needed because of difference in `invoke` and sam calls
                invokeSymbol.owner.extensionReceiverParameter = invokeSymbol.owner.valueParameters[0]
                invokeSymbol.owner.valueParameters = invokeSymbol.owner.valueParameters.drop(1)
            }
        }
    private var _parameters: List<KParameter>? = null
    private var _returnType: KType? = null
    private var _typeParameters: List<KTypeParameter>? = null

    val invokeSymbol: IrFunctionSymbol = run {
        val hasDispatchReceiver = irFunction.dispatchReceiverParameter?.let { getField(it.symbol) } != null
        val hasExtensionReceiver = irFunction.extensionReceiverParameter?.let { getField(it.symbol) } != null
        environment.getCachedFunction(irFunction.symbol, hasDispatchReceiver, hasExtensionReceiver) ?: environment.setCachedFunction(
            irFunction.symbol, hasDispatchReceiver, hasExtensionReceiver,
            newFunction = createInvokeFunction(irFunction, irClass, hasDispatchReceiver, hasExtensionReceiver).symbol
        )
    }

    companion object {
        private fun createInvokeFunction(
            irFunction: IrFunction, irClass: IrClass, hasDispatchReceiver: Boolean, hasExtensionReceiver: Boolean
        ): IrSimpleFunction {
            val invokeFunction = irClass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .single { it.name == OperatorNameConventions.INVOKE }
            // TODO do we need new class here? if yes, do we need different names for temp classes?
            val functionClass = createTempClass(Name.identifier("Function\$0")).apply { parent = irFunction.parent }

            functionClass.superTypes += irClass.defaultType
            val newFunctionToInvoke = createTempFunction(
                OperatorNameConventions.INVOKE, irFunction.returnType, TEMP_FUNCTION_FOR_INTERPRETER
            ).apply impl@{
                parent = functionClass
                overriddenSymbols = listOf(invokeFunction.symbol)

                dispatchReceiverParameter = invokeFunction.dispatchReceiverParameter?.deepCopyWithSymbols(initialParent = this)
                val newValueParameters = mutableListOf<IrValueParameter>()

                val call = when (irFunction) {
                    is IrSimpleFunction -> irFunction.createCall()
                    is IrConstructor -> irFunction.createConstructorCall()
                    else -> TODO("Unsupported symbol $symbol for invoke")
                }.apply {
                    val dispatchParameter = irFunction.dispatchReceiverParameter
                    val extensionParameter = irFunction.extensionReceiverParameter

                    if (dispatchParameter != null) {
                        dispatchReceiver = dispatchParameter.createGetValue()
                        if (!hasDispatchReceiver) newValueParameters += dispatchParameter
                    }
                    if (extensionParameter != null) {
                        extensionReceiver = extensionParameter.createGetValue()
                        if (!hasExtensionReceiver) newValueParameters += extensionParameter
                    }
                    irFunction.valueParameters.forEach {
                        putArgument(it, it.createGetValue())
                        newValueParameters += it
                    }
                }

                valueParameters = newValueParameters
                body = listOf(this.createReturn(call)).wrapWithBlockBody()
            }
            functionClass.declarations += newFunctionToInvoke
            return newFunctionToInvoke
        }

        private fun isCallToNonAbstractMethodOfFunInterface(expression: IrCall): Boolean {
            val owner = expression.symbol.owner
            return owner.hasFunInterfaceParent() && owner.modality != Modality.ABSTRACT
        }

        fun isCallToInvokeOrMethodFromFunInterface(expression: IrCall): Boolean {
            val owner = expression.symbol.owner
            return owner.name == OperatorNameConventions.INVOKE || owner.hasFunInterfaceParent()
        }
    }

    constructor(
        functionReference: IrFunctionReference,
        environment: IrInterpreterEnvironment,
        dispatchReceiver: Field?,
        extensionReceiver: Field?
    ) : this(
        functionReference.symbol.owner,
        functionReference.type.classOrNull!!.owner,
        environment,
        listOfNotNull(dispatchReceiver, extensionReceiver).toMap().toMutableMap()
    ) {
        dispatchReceiver?.let { (symbol, state) -> setField(symbol, state) }
        extensionReceiver?.let { (symbol, state) -> setField(symbol, state) }
        // receivers are used in comparison of two functions in KFunctionProxy
        upValues += fields.map { it.key to Variable(it.value) }
    }

    override fun getIrFunctionByIrCall(expression: IrCall): IrFunction? {
        if (isCallToNonAbstractMethodOfFunInterface(expression)) return expression.symbol.owner.resolveFakeOverride()
        if (isCallToInvokeOrMethodFromFunInterface(expression)) return invokeSymbol.owner
        return super.getIrFunctionByIrCall(expression)
    }

    fun getParameters(callInterceptor: CallInterceptor): List<KParameter> {
        if (_parameters != null) return _parameters!!
        val kParameterIrClass = callInterceptor.environment.kParameterClass.owner
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
        val kTypeIrClass = callInterceptor.environment.kTypeClass.owner
        _returnType = KTypeProxy(KTypeState(irFunction.returnType, kTypeIrClass), callInterceptor)
        return _returnType!!
    }

    fun getTypeParameters(callInterceptor: CallInterceptor): List<KTypeParameter> {
        if (_typeParameters != null) return _typeParameters!!
        val kTypeParametersIrClass = callInterceptor.environment.kTypeParameterClass.owner
        _typeParameters = irClass.typeParameters.map { KTypeParameterProxy(KTypeParameterState(it, kTypeParametersIrClass), callInterceptor) }
        return _typeParameters!!
    }

    fun getArity(): Int? {
        return irClass.name.asString()
            .removePrefix("Suspend").removePrefix("Function").removePrefix("KFunction")
            .toIntOrNull()
    }

    private fun isLambda(): Boolean = irFunction.name.let { it == SpecialNames.ANONYMOUS || it == SpecialNames.NO_NAME_PROVIDED }

    override fun toString(): String {
        return if (isLambda()) renderLambda(irFunction) else renderFunction(irFunction)
    }
}
