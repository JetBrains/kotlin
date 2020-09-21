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
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.*
import org.jetbrains.kotlin.ir.types.classOrNull
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter

internal class KClassState(val classReference: IrClass, override val irClass: IrClass) : ReflectionState() {
    private var _members: Collection<KCallable<*>>? = null
    private var _constructors: Collection<KFunction<*>>? = null
    private var _typeParameters: List<KTypeParameter>? = null
    private var _supertypes: List<KType>? = null

    constructor(classReference: IrClassReference) : this(classReference.symbol.owner as IrClass, classReference.type.classOrNull!!.owner)

    fun getMembers(interpreter: IrInterpreter): Collection<KCallable<*>> {
        if (_members != null) return _members!!
        _members = classReference.declarations
            .filter { it !is IrClass && it !is IrConstructor }
            .map {
                when (it) {
                    is IrProperty -> {
                        val withExtension = it.getter?.extensionReceiverParameter != null
                        when {
                            !withExtension && !it.isVar ->
                                KProperty1Proxy(KPropertyState(it, interpreter.irBuiltIns.getKPropertyClass(false, 1).owner), interpreter)
                            !withExtension && it.isVar ->
                                KMutableProperty1Proxy(
                                    KPropertyState(it, interpreter.irBuiltIns.getKPropertyClass(true, 1).owner), interpreter
                                )
                            withExtension && !it.isVar ->
                                KProperty2Proxy(KPropertyState(it, interpreter.irBuiltIns.getKPropertyClass(false, 2).owner), interpreter)
                            !withExtension && it.isVar ->
                                KMutableProperty2Proxy(
                                    KPropertyState(it, interpreter.irBuiltIns.getKPropertyClass(true, 2).owner), interpreter
                                )
                            else -> TODO()
                        }
                    }
                    is IrFunction -> KFunctionProxy(KFunctionState(it, interpreter.irBuiltIns.functionFactory), interpreter)
                    else -> TODO()
                }
            }
        return _members!!
    }

    fun getConstructors(interpreter: IrInterpreter): Collection<KFunction<*>> {
        if (_constructors != null) return _constructors!!
        _constructors = classReference.declarations
            .filterIsInstance<IrConstructor>()
            .map { KFunctionProxy(KFunctionState(it, interpreter.irBuiltIns.functionFactory), interpreter) }
        return _constructors!!
    }

    fun getTypeParameters(interpreter: IrInterpreter): List<KTypeParameter> {
        if (_typeParameters != null) return _typeParameters!!
        val kTypeParameterIrClass = irClass.getIrClassOfReflectionFromList("typeParameters")
        _typeParameters = classReference.typeParameters.map { KTypeParameterProxy(KTypeParameterState(it, kTypeParameterIrClass), interpreter) }
        return _typeParameters!!
    }

    fun getSupertypes(interpreter: IrInterpreter): List<KType> {
        if (_supertypes != null) return _supertypes!!
        val kTypeIrClass = irClass.getIrClassOfReflectionFromList("supertypes")
        _supertypes = (classReference.superTypes.map { it } + interpreter.irBuiltIns.anyType).toSet()
            .map { KTypeProxy(KTypeState(it, kTypeIrClass), interpreter) }
        return _supertypes!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KClassState

        if (classReference != other.classReference) return false

        return true
    }

    override fun hashCode(): Int {
        return classReference.hashCode()
    }

    override fun toString(): String {
        return "class ${classReference.internalName()}"
    }
}