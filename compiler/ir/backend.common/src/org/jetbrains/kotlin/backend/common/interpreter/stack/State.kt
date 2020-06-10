/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.stack

import org.jetbrains.kotlin.backend.common.interpreter.builtins.CompileTimeFunction
import org.jetbrains.kotlin.backend.common.interpreter.builtins.unaryFunctions
import org.jetbrains.kotlin.backend.common.interpreter.equalTo
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization

interface State {
    val fields: MutableList<Variable>

    fun getState(descriptor: DeclarationDescriptor): State? {
        return fields.firstOrNull { it.descriptor.equalTo(descriptor) }?.state
    }

    fun setState(newVar: Variable)
    fun copy(): State
    fun getIrFunction(descriptor: FunctionDescriptor): IrFunction?
}

class Primitive<T>(private var value: T, private val type: IrType?) : State {
    override val fields: MutableList<Variable> = mutableListOf()

    init {
        if (type != null) {
            val properties = type.classOrNull!!.owner.declarations.filterIsInstance<IrProperty>()
            for (property in properties) {
                val propertySignature = CompileTimeFunction(property.name.asString(), listOf(type.getName()))
                val propertyValue = unaryFunctions[propertySignature]?.invoke(value)
                    ?: throw NoSuchMethodException("For given property $propertySignature there is no entry in unary map")
                fields.add(Variable(property.descriptor, Primitive(propertyValue, null)))
            }
        }
    }

    private fun IrType?.getName() = this?.classOrNull!!.owner.name.asString()

    fun getValue(): T {
        return value
    }

    override fun setState(newVar: Variable) {
        newVar.state as? Primitive<T> ?: throw IllegalArgumentException("Cannot set $newVar in current $this")
        value = newVar.state.value
    }

    override fun copy(): State {
        return Primitive(value, type)
    }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        if (type == null) return null
        // must add property's getter to declaration's list because they are not present in ir class for primitives
        val declarations = type.classOrNull!!.owner.declarations.map { if (it is IrProperty) it.getter else it }
        return declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name == descriptor.name }
            .firstOrNull { it.descriptor.valueParameters.map { it.type } == descriptor.valueParameters.map { it.type } }
    }

    override fun toString(): String {
        return "Primitive(value=$value, type=${type?.getName()})"
    }
}

class Complex(private var type: IrClass, override val fields: MutableList<Variable>) : State {
    var superType: Complex? = null
    var instance: Complex? = null

    fun setInstanceRecursive(instance: Complex) {
        this.instance = instance
        superType?.setInstanceRecursive(instance)
    }

    fun getReceiver(): DeclarationDescriptor {
        return type.thisReceiver!!.descriptor
    }

    override fun setState(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.descriptor == newVar.descriptor }) {
            null -> fields.add(newVar)                          // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)] = newVar   // newVar already present
        }
    }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        return type.declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name == descriptor.name }
            .firstOrNull { it.descriptor.valueParameters.map { it.type } == descriptor.valueParameters.map { it.type } }
    }

    override fun copy(): State {
        return Complex(type, fields).apply {
            this@apply.superType = this@Complex.superType
            this@apply.instance = this@Complex.instance
        }
    }

    override fun toString(): String {
        return "Complex(obj='${type.fqNameForIrSerialization}', super=$superType, values=$fields)"
    }
}
