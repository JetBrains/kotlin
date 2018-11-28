/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen.optimization.boxing

import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.isRangeOrProgression
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.InlineClassDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import java.util.*

open class BoxingInterpreter(
    private val insnList: InsnList,
    private val generationState: GenerationState
) : OptimizationBasicInterpreter() {
    private val boxingPlaces = HashMap<Int, BoxedBasicValue>()

    protected open fun createNewBoxing(
        insn: AbstractInsnNode,
        type: Type,
        progressionIterator: ProgressionIteratorBasicValue?
    ): BasicValue =
        boxingPlaces.getOrPut(insnList.indexOf(insn)) {
            val boxedBasicValue = CleanBoxedValue(type, insn, progressionIterator, generationState)
            onNewBoxedValue(boxedBasicValue)
            boxedBasicValue
        }

    protected fun checkUsedValue(value: BasicValue) {
        if (value is TaintedBoxedValue) {
            onMergeFail(value)
        }
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<BasicValue>): BasicValue? {
        values.forEach {
            checkUsedValue(it)
        }

        val value = super.naryOperation(insn, values)
        val firstArg = values.firstOrNull() ?: return value

        return when {
            insn.isBoxing(generationState) -> {
                /*
                * It's possible to have chain of several boxings and it's important to retain these boxing methods, consider:
                *
                * inline class AsAny(val a: Any)
                *
                * fun takeAny(a: Any)
                *
                * fun foo() {
                *   takeAny(AsAny(42)) // valueOf -> AsAny$Erased.box
                * }
                *
                * */
                values.markBoxedArgumentValues()
                createNewBoxing(insn, value.type, null)
            }
            insn.isUnboxing(generationState) && firstArg is BoxedBasicValue -> {
                onUnboxing(insn, firstArg, value.type)
                value
            }
            insn.isIteratorMethodCallOfProgression(values) ->
                ProgressionIteratorBasicValue.byProgressionClassType(firstArg.type)
            insn.isNextMethodCallOfProgressionIterator(values) -> {
                val progressionIterator = firstArg as? ProgressionIteratorBasicValue
                        ?: throw AssertionError("firstArg should be progression iterator")
                createNewBoxing(insn, AsmUtil.boxType(progressionIterator.valuesPrimitiveType), progressionIterator)
            }
            insn.isAreEqualIntrinsicForSameTypedBoxedValues(values) && canValuesBeUnboxedForAreEqual(values, generationState) -> {
                onAreEqual(insn, values[0] as BoxedBasicValue, values[1] as BoxedBasicValue)
                value
            }
            insn.isJavaLangComparableCompareToForSameTypedBoxedValues(values) -> {
                onCompareTo(insn, values[0] as BoxedBasicValue, values[1] as BoxedBasicValue)
                value
            }
            else -> {
                // N-ary operation should be a method call or multinewarray.
                // Arguments for multinewarray could be only numeric,
                // so if there are boxed values in args, it's not a case of multinewarray.
                values.markBoxedArgumentValues()
                value
            }
        }
    }

    private fun List<BasicValue>.markBoxedArgumentValues() {
        for (arg in this) {
            if (arg is BoxedBasicValue) {
                onMethodCallWithBoxedValue(arg)
            }
        }
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? {
        checkUsedValue(value)

        return if (insn.opcode == Opcodes.CHECKCAST && isExactValue(value))
            value
        else
            super.unaryOperation(insn, value)
    }

    protected open fun isExactValue(value: BasicValue) =
        value is ProgressionIteratorBasicValue ||
                value is CleanBoxedValue ||
                value.type != null && isProgressionClass(value.type)

    override fun merge(v: BasicValue, w: BasicValue) =
        when {
            v == StrictBasicValue.UNINITIALIZED_VALUE || w == StrictBasicValue.UNINITIALIZED_VALUE ->
                StrictBasicValue.UNINITIALIZED_VALUE
            v is BoxedBasicValue && w is BoxedBasicValue -> {
                onMergeSuccess(v, w)
                when {
                    v is TaintedBoxedValue -> v
                    w is TaintedBoxedValue -> w
                    v.type != w.type -> v.taint()
                    else -> v
                }
            }
            v is BoxedBasicValue ->
                v.taint()
            w is BoxedBasicValue ->
                w.taint()
            else ->
                super.merge(v, w)
        }

    protected open fun onNewBoxedValue(value: BoxedBasicValue) {}
    protected open fun onUnboxing(insn: AbstractInsnNode, value: BoxedBasicValue, resultType: Type) {}
    protected open fun onAreEqual(insn: AbstractInsnNode, value1: BoxedBasicValue, value2: BoxedBasicValue) {}
    protected open fun onCompareTo(insn: AbstractInsnNode, value1: BoxedBasicValue, value2: BoxedBasicValue) {}
    protected open fun onMethodCallWithBoxedValue(value: BoxedBasicValue) {}
    protected open fun onMergeFail(value: BoxedBasicValue) {}
    protected open fun onMergeSuccess(v: BoxedBasicValue, w: BoxedBasicValue) {}

}

private val UNBOXING_METHOD_NAMES =
    ImmutableSet.of("booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue")

private val KCLASS_TO_JLCLASS = Type.getMethodDescriptor(AsmTypes.JAVA_CLASS_TYPE, AsmTypes.K_CLASS_TYPE)
private val JLCLASS_TO_KCLASS = Type.getMethodDescriptor(AsmTypes.K_CLASS_TYPE, AsmTypes.JAVA_CLASS_TYPE)

fun AbstractInsnNode.isUnboxing(state: GenerationState) =
    isPrimitiveUnboxing() || isJavaLangClassUnboxing() || isInlineClassUnboxing(state)

fun AbstractInsnNode.isBoxing(state: GenerationState) =
    isPrimitiveBoxing() || isJavaLangClassBoxing() || isInlineClassBoxing(state)

fun AbstractInsnNode.isPrimitiveUnboxing() =
    isMethodInsnWith(Opcodes.INVOKEVIRTUAL) {
        isWrapperClassNameOrNumber(owner) && isUnboxingMethodName(name)
    }

fun AbstractInsnNode.isJavaLangClassUnboxing() =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        owner == "kotlin/jvm/JvmClassMappingKt" &&
                name == "getJavaClass" &&
                desc == KCLASS_TO_JLCLASS
    }

inline fun AbstractInsnNode.isMethodInsnWith(opcode: Int, condition: MethodInsnNode.() -> Boolean): Boolean =
    this.opcode == opcode && this is MethodInsnNode && this.condition()

private fun isWrapperClassNameOrNumber(internalClassName: String) =
    isWrapperClassName(internalClassName) || internalClassName == Type.getInternalName(Number::class.java)

private fun isWrapperClassName(internalClassName: String) =
    JvmPrimitiveType.isWrapperClassName(buildFqNameByInternal(internalClassName))


private fun buildFqNameByInternal(internalClassName: String) =
    FqName(Type.getObjectType(internalClassName).className)

private fun isUnboxingMethodName(name: String) =
    UNBOXING_METHOD_NAMES.contains(name)

fun AbstractInsnNode.isPrimitiveBoxing() =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        isWrapperClassName(owner) &&
                name == "valueOf" &&
                isBoxingMethodDescriptor()
    }

private fun MethodInsnNode.isBoxingMethodDescriptor(): Boolean {
    val ownerType = Type.getObjectType(owner)
    return desc == Type.getMethodDescriptor(ownerType, AsmUtil.unboxType(ownerType))
}

fun AbstractInsnNode.isJavaLangClassBoxing() =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        owner == AsmTypes.REFLECTION &&
                name == "getOrCreateKotlinClass" &&
                desc == JLCLASS_TO_KCLASS
    }

private fun AbstractInsnNode.isInlineClassBoxing(state: GenerationState) =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        isInlineClassBoxingMethodDescriptor(state)
    }

private fun AbstractInsnNode.isInlineClassUnboxing(state: GenerationState) =
    isMethodInsnWith(Opcodes.INVOKEVIRTUAL) {
        isInlineClassUnboxingMethodDescriptor(state)
    }

private fun MethodInsnNode.isInlineClassBoxingMethodDescriptor(state: GenerationState): Boolean {
    if (name != KotlinTypeMapper.BOX_JVM_METHOD_NAME) return false

    val ownerType = Type.getObjectType(owner)
    val descriptor = state.jvmBackendClassResolver.resolveToClassDescriptors(ownerType).singleOrNull() ?: return false

    if (!descriptor.isInline) return false

    return desc == Type.getMethodDescriptor(ownerType, state.typeMapper.mapType(descriptor.defaultType))
}

private fun MethodInsnNode.isInlineClassUnboxingMethodDescriptor(state: GenerationState): Boolean {
    if (name != KotlinTypeMapper.UNBOX_JVM_METHOD_NAME) return false

    val ownerType = Type.getObjectType(owner)
    val descriptor = state.jvmBackendClassResolver.resolveToClassDescriptors(ownerType).singleOrNull() ?: return false

    if (!descriptor.isInline) return false

    return desc == Type.getMethodDescriptor(state.typeMapper.mapType(descriptor.defaultType))
}

fun AbstractInsnNode.isNextMethodCallOfProgressionIterator(values: List<BasicValue>) =
    values.firstOrNull() is ProgressionIteratorBasicValue &&
            isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
                name == "next"
            }

fun AbstractInsnNode.isIteratorMethodCallOfProgression(values: List<BasicValue>) =
    isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
        val firstArgType = values.firstOrNull()?.type
        firstArgType != null &&
                isProgressionClass(firstArgType) &&
                name == "iterator"
    }

fun isProgressionClass(type: Type) =
    isRangeOrProgression(buildFqNameByInternal(type.internalName))

fun AbstractInsnNode.isAreEqualIntrinsicForSameTypedBoxedValues(values: List<BasicValue>) =
    isAreEqualIntrinsic() && areSameTypedPrimitiveBoxedValues(values)

fun areSameTypedPrimitiveBoxedValues(values: List<BasicValue>): Boolean {
    if (values.size != 2) return false
    val (v1, v2) = values
    return v1 is BoxedBasicValue &&
            v2 is BoxedBasicValue &&
            v1.descriptor.unboxedType == v2.descriptor.unboxedType &&
            !v1.descriptor.isInlineClassValue && !v2.descriptor.isInlineClassValue
}

fun AbstractInsnNode.isAreEqualIntrinsic() =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        name == "areEqual" &&
                owner == IntrinsicMethods.INTRINSICS_CLASS_NAME &&
                desc == "(Ljava/lang/Object;Ljava/lang/Object;)Z"
    }

private val shouldUseEqualsForWrappers = setOf(Type.DOUBLE_TYPE, Type.FLOAT_TYPE, AsmTypes.JAVA_CLASS_TYPE)

fun canValuesBeUnboxedForAreEqual(values: List<BasicValue>, generationState: GenerationState): Boolean =
    values.none { getUnboxedType(it.type, generationState) in shouldUseEqualsForWrappers }

fun AbstractInsnNode.isJavaLangComparableCompareToForSameTypedBoxedValues(values: List<BasicValue>) =
    isJavaLangComparableCompareTo() && areSameTypedPrimitiveBoxedValues(values)

fun AbstractInsnNode.isJavaLangComparableCompareTo() =
    isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
        name == "compareTo" &&
                owner == "java/lang/Comparable" &&
                desc == "(Ljava/lang/Object;)I"
    }
