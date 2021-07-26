/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.types.classOrNull
import kotlin.reflect.KParameter
import kotlin.reflect.KType

internal class KPropertyState(val property: IrProperty, override val irClass: IrClass, val receiver: State? = null) : ReflectionState() {
    constructor(propertyReference: IrPropertyReference, receiver: State?)
            : this(propertyReference.symbol.owner, propertyReference.type.classOrNull!!.owner, receiver)

    private var _parameters: List<KParameter>? = null
    private var _returnType: KType? = null

    fun convertGetterToKFunctionState(environment: IrInterpreterEnvironment): KFunctionState {
        val getterClass = irClass.getIrClassOfReflection("getter")
        val functionType = getterClass.superTypes.single { it.classOrNull?.owner?.name?.asString() == "Function1" }
        return KFunctionState(property.getter!!, functionType.classOrNull!!.owner, environment)
    }

    fun getParameters(callInterceptor: CallInterceptor): List<KParameter> {
        if (_parameters != null) return _parameters!!
        val kParameterIrClass = irClass.getIrClassOfReflectionFromList("parameters")
        var index = 0
        val instanceParameter = property.getter?.dispatchReceiverParameter?.takeIf { receiver == null }
            ?.let { KParameterProxy(KParameterState(kParameterIrClass, it, index++, KParameter.Kind.INSTANCE), callInterceptor) }
        val extensionParameter = property.getter?.extensionReceiverParameter
            ?.let { KParameterProxy(KParameterState(kParameterIrClass, it, index++, KParameter.Kind.EXTENSION_RECEIVER), callInterceptor) }
        _parameters = listOfNotNull(instanceParameter, extensionParameter)
        return _parameters!!
    }

    fun getReturnType(callInterceptor: CallInterceptor): KType {
        if (_returnType != null) return _returnType!!
        val kTypeIrClass = irClass.getIrClassOfReflection("returnType")
        _returnType = KTypeProxy(KTypeState(property.getter!!.returnType, kTypeIrClass), callInterceptor)
        return _returnType!!
    }

    fun isKProperty0(): Boolean = irClass.name.asString() == "KProperty0"

    fun isKProperty1(): Boolean = irClass.name.asString() == "KProperty1"

    fun isKProperty2(): Boolean = irClass.name.asString() == "KProperty2"

    fun isKMutableProperty0(): Boolean = irClass.name.asString() == "KMutableProperty0"

    fun isKMutableProperty1(): Boolean = irClass.name.asString() == "KMutableProperty1"

    fun isKMutableProperty2(): Boolean = irClass.name.asString() == "KMutableProperty2"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KPropertyState

        if (property != other.property) return false
        if (receiver != other.receiver) return false

        return true
    }

    override fun hashCode(): Int {
        var result = property.hashCode()
        result = 31 * result + (receiver?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return renderProperty(property)
    }
}