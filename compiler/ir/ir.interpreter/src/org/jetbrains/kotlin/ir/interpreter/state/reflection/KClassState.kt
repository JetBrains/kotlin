/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state.reflection

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.internalName
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KFunctionProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KProperty1Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeParameterProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.KTypeProxy
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

internal class KClassState(override val irClass: IrClass) : ReflectionState(irClass.symbol) {
    private var _members: Collection<KCallable<*>>? = null
    private var _constructors: Collection<KFunction<*>>? = null
    private var _typeParameters: List<KTypeParameter>? = null
    private var _supertypes: List<KType>? = null

    constructor(classReference: IrClassReference) : this(classReference.symbol.owner as IrClass)

    fun getMembers(interpreter: IrInterpreter): Collection<KCallable<*>> {
        if (_members != null) return _members!!
        _members = irClass.declarations
            .filter { it !is IrClass && it !is IrConstructor }
            .map {
                when (it) {
                    is IrProperty -> KProperty1Proxy(KPropertyState(it, null, null), interpreter) // TODO KProperty2
                    is IrFunction -> KFunctionProxy(KFunctionState(it, interpreter.irBuiltIns.functionFactory), interpreter)
                    else -> TODO()
                }
            }
        return _members!!
    }

    fun getConstructors(interpreter: IrInterpreter): Collection<KFunction<*>> {
        if (_constructors != null) return _constructors!!
        _constructors = irClass.declarations
            .filterIsInstance<IrConstructor>()
            .map { KFunctionProxy(KFunctionState(it, interpreter.irBuiltIns.functionFactory), interpreter) }
        return _constructors!!
    }

    fun getTypeParameters(interpreter: IrInterpreter): List<KTypeParameter> {
        if (_typeParameters != null) return _typeParameters!!
        _typeParameters = irClass.typeParameters.map { KTypeParameterProxy(KTypeParameterState(it), interpreter) }
        return _typeParameters!!
    }

    fun getSupertypes(interpreter: IrInterpreter): List<KType> {
        if (_supertypes != null) return _supertypes!!
        _supertypes = (irClass.superTypes.map { it } + interpreter.irBuiltIns.anyType).toSet()
            .map { KTypeProxy(KTypeState(it), interpreter) }
        return _supertypes!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KClassState

        if (irClass != other.irClass) return false

        return true
    }

    override fun hashCode(): Int {
        return irClass.hashCode()
    }

    override fun toString(): String {
        return "class ${irClass.internalName()}"
    }
}