/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.state

import org.jetbrains.kotlin.backend.common.interpreter.equalTo
import org.jetbrains.kotlin.backend.common.interpreter.stack.Variable
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization

abstract class Complex(
    override val irClass: IrClass, override val fields: MutableList<Variable>, var superClass: Complex?, var subClass: Complex?
) : State {
    fun setSuperClassInstance(superClass: Complex) {
        this.superClass = superClass
        superClass.subClass = this
    }

    fun getOriginal(): Complex {
        return subClass?.getOriginal() ?: this
    }

    fun irClassFqName(): String {
        return irClass.fqNameForIrSerialization.toString()
    }

    override fun setState(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.descriptor == newVar.descriptor }) {
            null -> fields.add(newVar)                          // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)] = newVar   // newVar already present
        }
    }

    fun setStatesFrom(state: Complex) = state.fields.forEach { setState(it) }

    override fun getIrFunction(descriptor: FunctionDescriptor): IrFunction? {
        val propertyGetters = irClass.declarations.filterIsInstance<IrProperty>().mapNotNull { it.getter }
        val functions = irClass.declarations.filterIsInstance<IrFunction>()
        return (propertyGetters + functions).singleOrNull { it.descriptor.equalTo(descriptor) }
    }
}