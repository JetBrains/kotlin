/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.ir.expressions.IrConst

interface State {
    fun getState(descriptor: DeclarationDescriptor): State
    fun setState(newState: State)
    fun getDescriptor(): DeclarationDescriptor
    fun setDescriptor(descriptor: DeclarationDescriptor): State
    fun isTypeOf(descriptor: DeclarationDescriptor): Boolean
    fun copy(): State
}

class Primitive<T>(private var value: IrConst<T>) : State {
    private lateinit var declarationDescriptor: DeclarationDescriptor

    constructor(descriptor: DeclarationDescriptor, value: IrConst<T>) : this(value) {
        declarationDescriptor = descriptor
    }

    fun getIrConst(): IrConst<T> {
        return value
    }

    override fun getState(descriptor: DeclarationDescriptor): State {
        return when (descriptor) {
            this.declarationDescriptor -> this
            else -> throw IllegalAccessException("Can't get descriptor $descriptor from $this")
        }
    }

    override fun setState(newState: State) {
        newState as? Primitive<T> ?: throw IllegalArgumentException("Cannot set $newState in current $this")
        value = newState.value
    }

    override fun getDescriptor(): DeclarationDescriptor {
        return declarationDescriptor
    }

    override fun setDescriptor(descriptor: DeclarationDescriptor): State {
        declarationDescriptor = descriptor
        return this
    }

    override fun isTypeOf(descriptor: DeclarationDescriptor): Boolean {
        return declarationDescriptor == descriptor
    }

    override fun copy(): State {
        return Primitive(declarationDescriptor, value)
    }

    override fun toString(): String {
        return "Primitive(varName='${declarationDescriptor.name}', value=${value.value})"
    }
}
class Complex(private var declarationDescriptor: DeclarationDescriptor, private val values: MutableList<State>) : State {
    private val superQualifiers = mutableListOf<Complex>()

    public fun addSuperQualifier(superObj: Complex) {
        superQualifiers += superObj.superQualifiers
        superQualifiers += superObj
    }

    override fun getState(descriptor: DeclarationDescriptor): State {
        return (values + superQualifiers).first { it.getDescriptor() == descriptor }
    }

    override fun setState(newState: State) {
        val oldState = values.firstOrNull { it.getDescriptor() == newState.getDescriptor() }
        if (oldState == null) {
            values.add(newState)
        } else {
            values[values.indexOf(oldState)] = newState
        }
    }

    override fun getDescriptor(): DeclarationDescriptor {
        return declarationDescriptor
    }

    override fun setDescriptor(descriptor: DeclarationDescriptor): State {
        declarationDescriptor = descriptor
        return this
    }

    override fun isTypeOf(descriptor: DeclarationDescriptor): Boolean {
        return (superQualifiers + this).any { it.declarationDescriptor == descriptor }
    }

    override fun copy(): State {
        return Complex(declarationDescriptor, values).apply { this.superQualifiers += this@Complex.superQualifiers }
    }

    override fun toString(): String {
        return "Complex(obj='$declarationDescriptor', super=$superQualifiers, values=$values)"
    }
}