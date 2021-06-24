/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.createCall
import org.jetbrains.kotlin.ir.interpreter.fqName
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.types.isNullableAny
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.resolveFakeOverride
import org.jetbrains.kotlin.name.Name

internal class Common private constructor(override val irClass: IrClass, override val fields: MutableList<Variable>) : Complex, StateWithClosure {
    override val upValues: MutableList<Variable> = mutableListOf()
    override var superWrapperClass: Wrapper? = null
    override var outerClass: Variable? = null

    constructor(irClass: IrClass) : this(irClass, mutableListOf())

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

    fun getEqualsFunction(): IrSimpleFunction {
        return irClass.declarations
            .filterIsInstance<IrSimpleFunction>()
            .single {
                it.name == Name.identifier("equals") && it.dispatchReceiverParameter != null
                        && it.valueParameters.size == 1 && it.valueParameters[0].type.isNullableAny()
            }
            .let { it.resolveFakeOverride() as IrSimpleFunction }
    }

    fun getHashCodeFunction(): IrSimpleFunction {
        return irClass.declarations.filterIsInstance<IrSimpleFunction>()
            .single { it.name.asString() == "hashCode" && it.valueParameters.isEmpty() }
            .let { it.resolveFakeOverride() as IrSimpleFunction }
    }

    fun getToStringFunction(): IrSimpleFunction {
        return irClass.declarations.filterIsInstance<IrSimpleFunction>()
            .single { it.name.asString() == "toString" && it.valueParameters.isEmpty() }
            .let { it.resolveFakeOverride() as IrSimpleFunction }
    }

    fun createToStringIrCall(): IrCall {
        return getToStringFunction().createCall()
    }

    override fun toString(): String {
        return "Common(obj='${irClass.fqName}', values=$fields)"
    }
}