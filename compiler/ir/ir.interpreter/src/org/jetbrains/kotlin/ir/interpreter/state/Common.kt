/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization

internal class Common private constructor(
    override val irClass: IrClass, override val fields: MutableList<Variable>
) : Complex(irClass, fields) {

    constructor(irClass: IrClass) : this(irClass, mutableListOf())

    override fun toString(): String {
        return "Common(obj='${irClass.fqNameForIrSerialization}', values=$fields)"
    }

    fun copyFieldsFrom(state: Complex) {
        this.fields.addAll(state.fields)
        superWrapperClass = state.superWrapperClass ?: state as? Wrapper
    }
}