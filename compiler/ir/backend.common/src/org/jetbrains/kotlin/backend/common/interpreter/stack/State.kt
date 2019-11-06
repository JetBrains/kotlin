/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConst

interface State {
    fun getState(): List<State>
    fun getDescriptor(): DeclarationDescriptor?
    fun setDescriptor(descriptor: DeclarationDescriptor): State
    fun copy(): State
}

class Primitive<T>(private val value: IrConst<T>) : State {
    private lateinit var declarationDescriptor: DeclarationDescriptor

    private constructor(descriptor: DeclarationDescriptor, value: IrConst<T>) : this(value) {
        declarationDescriptor = descriptor
    }

    override fun getState(): List<State> {
        return listOf(this)
    }

    override fun getDescriptor(): DeclarationDescriptor? {
        return declarationDescriptor
    }

    override fun setDescriptor(descriptor: DeclarationDescriptor): State {
        declarationDescriptor = descriptor
        return this
    }

    override fun copy(): State {
        return Primitive(declarationDescriptor, value)
    }

    public fun getIrConst(): IrConst<T> {
        return value
    }

    override fun toString(): String {
        return "Primitive(varName='${declarationDescriptor.name}', value=${value.value})"
    }
}
class Complex(private var declarationDescriptor: DeclarationDescriptor?, private val values: List<State>) : State {
    override fun getState(): List<State> {
        return values
    }

    override fun getDescriptor(): DeclarationDescriptor? {
        return declarationDescriptor
    }

    override fun setDescriptor(descriptor: DeclarationDescriptor): State {
        declarationDescriptor = descriptor
        return this
    }

    override fun copy(): State {
        return Complex(declarationDescriptor, values)
    }

    override fun toString(): String {
        return "Complex(objName='${declarationDescriptor?.name}', values=$values)"
    }
}