/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.AsmUtil.unboxPrimitiveTypeOrNull
import org.jetbrains.kotlin.codegen.StackValue.*
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.load.java.Constant
import org.jetbrains.kotlin.load.java.EnumEntry
import org.jetbrains.kotlin.load.java.descriptors.NullDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.StringDefaultValue
import org.jetbrains.kotlin.load.java.descriptors.getDefaultValueFromAnnotation
import org.jetbrains.kotlin.load.java.lexicalCastFrom
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.DFS
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

fun ValueParameterDescriptor.findJavaDefaultArgumentValue(targetType: Type, typeMapper: KotlinTypeMapper): StackValue {
    val descriptorWithDefaultValue = DFS.dfs(
        listOf(this.original),
        { it.original.overriddenDescriptors.map(ValueParameterDescriptor::getOriginal) },
        object : DFS.AbstractNodeHandler<ValueParameterDescriptor, ValueParameterDescriptor?>() {
            var result: ValueParameterDescriptor? = null

            override fun beforeChildren(current: ValueParameterDescriptor?): Boolean {
                if (current?.declaresDefaultValue() == true && current.getDefaultValueFromAnnotation() != null) {
                    result = current
                    return false
                }

                return true
            }

            override fun result(): ValueParameterDescriptor? = result
        }
    ) ?: error("Should be at least one descriptor with default value: $this")

    val defaultValue = descriptorWithDefaultValue.getDefaultValueFromAnnotation()
    if (defaultValue is NullDefaultValue) {
        return constant(null, targetType)
    }

    val value = (defaultValue as StringDefaultValue).value
    val castResult = type.lexicalCastFrom(value) ?: error("Should be checked in frontend")

    return when (castResult) {
        is EnumEntry -> enumEntry(castResult.descriptor, typeMapper)
        is Constant -> {
            val unboxedType = unboxPrimitiveTypeOrNull(targetType) ?: targetType
            return coercion(constant(castResult.value, unboxedType), targetType, null)
        }
    }
}
