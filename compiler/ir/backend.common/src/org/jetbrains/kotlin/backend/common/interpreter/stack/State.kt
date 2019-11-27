/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.equalTo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.Name

interface State {
    fun getState(descriptor: DeclarationDescriptor): State?
    fun setState(newVar: Variable)
    fun copy(): State
}

class Primitive<T>(private var value: IrConst<T>) : State {
    fun getIrConst(): IrConst<T> {
        return value
    }

    override fun getState(descriptor: DeclarationDescriptor): State {
        throw UnsupportedOperationException("Only complex are allowed")
    }

    override fun setState(newVar: Variable) {
        newVar.state as? Primitive<T> ?: throw IllegalArgumentException("Cannot set $newVar in current $this")
        value = newVar.state.value
    }

    override fun copy(): State {
        return Primitive(value)
    }

    override fun toString(): String {
        return "Primitive(value=${value.value})"
    }
}

class Complex(private var classOfObject: IrClass, private val values: MutableList<Variable>) : State {
    fun addSuperQualifier(superObj: Variable) {
        val superTypesList = getSuperTypes(classOfObject).map { it.descriptor.thisAsReceiverParameter }
        (superObj.state as? Complex)?.values?.filter { superTypesList.contains(it.descriptor) }?.let { values += it }
        values += superObj
    }

    private fun getSuperTypes(descriptor: IrClass): List<IrClassSymbol> {
        val superTypesList = descriptor.superTypes.mapNotNull { it.classOrNull }.toMutableList()
        return superTypesList + superTypesList.flatMap { getSuperTypes(it.owner) }
    }

    fun getIrFunctionByName(name: Name): IrFunction? {
        return classOfObject.declarations.filterIsInstance<IrFunction>().firstOrNull { it.descriptor.name == name }
    }

    override fun getState(descriptor: DeclarationDescriptor): State? {
        return values.firstOrNull { it.descriptor.equalTo(descriptor) }?.state
    }

    override fun setState(newVar: Variable) {
        when (val oldState = values.firstOrNull { it.descriptor == newVar.descriptor }) {
            null -> values.add(newVar)                          // newVar isn't present in value list
            else -> values[values.indexOf(oldState)] = newVar   // newVar already present
        }
    }

    override fun copy(): State {
        return Complex(classOfObject, values)
    }

    override fun toString(): String {
        return "Complex(obj='${classOfObject.fqNameForIrSerialization}', values=$values)"
    }
}

class EmptyState : State {
    override fun getState(descriptor: DeclarationDescriptor): State {
        throw UnsupportedOperationException("Get state is not supported in empty state object")
    }

    override fun setState(newVar: Variable) {
        throw UnsupportedOperationException("Set state is not supported in empty state object")
    }

    override fun copy(): State {
        throw UnsupportedOperationException("Copy method is not supported in empty state object")
    }

    override fun toString(): String {
        return "EmptyState"
    }
}