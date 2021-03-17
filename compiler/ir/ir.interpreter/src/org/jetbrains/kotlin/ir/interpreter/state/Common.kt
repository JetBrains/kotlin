/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

internal class Common private constructor(override val irClass: IrClass, override val fields: MutableList<Variable>) : Complex, StateWithClosure {
    override val upValues: MutableList<Variable> = mutableListOf()
    override var superWrapperClass: Wrapper? = null
    override var outerClass: Variable? = null

    constructor(irClass: IrClass) : this(irClass, mutableListOf())

    fun copyFieldsFrom(state: Complex) {
        this.fields.addAll(state.fields)
        superWrapperClass = state.superWrapperClass ?: state as? Wrapper
    }

    // This method is used to get correct java method name
    private fun getKotlinName(declaringClassName: String, methodName: String): String {
        return when {
            // TODO see specialBuiltinMembers.kt
            //"kotlin.collections.Map.<get-entries>" -> "entrySet"
            //"kotlin.collections.Map.<get-keys>" -> "keySet"
            declaringClassName == "java.lang.CharSequence" && methodName == "charAt" -> "get"
            //"kotlin.collections.MutableList.removeAt" -> "remove"
            else -> methodName
        }
    }

    fun getIrFunction(method: java.lang.reflect.Method): IrFunction? {
        val methodName = getKotlinName(method.declaringClass.name, method.name)
        return when (val declaration = irClass.declarations.singleOrNull { it.nameForIrSerialization.asString() == methodName }) {
            is IrProperty -> declaration.getter
            else -> declaration as? IrFunction
        }
    }

    override fun toString(): String {
        return "Common(obj='${irClass.fqNameForIrSerialization}', values=$fields)"
    }
}