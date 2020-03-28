/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter.state

import org.jetbrains.kotlin.backend.common.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization

class Common private constructor(
    override val irClass: IrClass, override val fields: MutableList<Variable>, superClass: Complex?, subClass: Complex?
) : Complex(irClass, fields, superClass, subClass) {

    constructor(irClass: IrClass) : this(irClass, mutableListOf(), null, null)

    fun getToStringFunction(): IrFunctionImpl {
        return irClass.declarations.filterIsInstance<IrFunction>()
            .filter { it.descriptor.name.asString() == "toString" }
            .first { it.descriptor.valueParameters.isEmpty() } as IrFunctionImpl
    }

    override fun copy(): State {
        return Common(irClass, fields, superClass, subClass ?: this)
    }

    override fun toString(): String {
        return "Common(obj='${irClass.fqNameForIrSerialization}', super=$superClass, values=$fields)"
    }
}