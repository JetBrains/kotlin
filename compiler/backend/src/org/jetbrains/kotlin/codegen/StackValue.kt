/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class CoercionValue(
    val value: StackValue,
    private val castType: Type,
    private val castKotlinType: KotlinType?,
    private val underlyingKotlinType: KotlinType? // type of the underlying parameter for inline class
) : StackValue(castType, castKotlinType, value.canHaveSideEffects()) {

    override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
        value.putSelector(value.type, value.kotlinType, v)

        // consider the following example:

        // inline class AsAny(val a: Any)
        // val a = AsAny(1)
        //
        // Here we should coerce `Int` (1) to `Any` and remember that resulting type is inline class type `AsAny` (not `Any`)
        StackValue.coerce(value.type, value.kotlinType, castType, underlyingKotlinType ?: castKotlinType, v)
        StackValue.coerce(castType, castKotlinType, type, kotlinType, v)
    }

    override fun storeSelector(topOfStackType: Type, topOfStackKotlinType: KotlinType?, v: InstructionAdapter) {
        value.storeSelector(topOfStackType, topOfStackKotlinType, v)
    }

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        value.putReceiver(v, isRead)
    }

    override fun isNonStaticAccess(isRead: Boolean): Boolean {
        return value.isNonStaticAccess(isRead)
    }
}


class StackValueWithLeaveTask(
    val stackValue: StackValue,
    val leaveTasks: (StackValue) -> Unit
) : StackValue(stackValue.type, stackValue.kotlinType) {

    override fun putReceiver(v: InstructionAdapter, isRead: Boolean) {
        stackValue.putReceiver(v, isRead)
    }

    override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
        stackValue.putSelector(type, kotlinType, v)
        leaveTasks(stackValue)
    }
}

open class OperationStackValue(
    resultType: Type,
    resultKotlinType: KotlinType?,
    val lambda: (v: InstructionAdapter) -> Unit
) : StackValue(resultType, resultKotlinType) {

    override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
        lambda(v)
        coerceTo(type, kotlinType, v)
    }
}

class FunctionCallStackValue(
    resultType: Type,
    resultKotlinType: KotlinType?,
    lambda: (v: InstructionAdapter) -> Unit
) : OperationStackValue(resultType, resultKotlinType, lambda)
