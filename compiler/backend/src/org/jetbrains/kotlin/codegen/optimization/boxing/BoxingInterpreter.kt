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
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.codegen.topLevelClassInternalName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.kotlin.types.*
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.TypeInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

abstract class BoxingInterpreter(
    private val insnList: InsnList,
    private val generationState: GenerationState
) : OptimizationBasicInterpreter() {
    private val boxingPlaces = HashMap<Int, BoxedBasicValue>()
    private val progressionIterators = HashMap<AbstractInsnNode, ProgressionIteratorBasicValue>()

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
            insn.isIteratorMethodCall() -> {
                values.markBoxedArgumentValues()
                val firstArgType = firstArg.type
                if (isProgressionClass(firstArgType)) {
                    progressionIterators.getOrPut(insn) {
                        ProgressionIteratorBasicValue.byProgressionClassType(insn, firstArgType)!!
                    }
                } else {
                    progressionIterators[insn]?.taint()
                    value
                }
            }
            insn.isNextMethodCallOfProgressionIterator(values) -> {
                val progressionIterator = firstArg as? ProgressionIteratorBasicValue
                    ?: throw AssertionError("firstArg should be progression iterator")
                createNewBoxing(insn, progressionIterator.boxedElementType, progressionIterator)
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

        return if (insn.opcode == Opcodes.CHECKCAST
            && isExactValue(value)
            && !isCastToProgression(insn) // operations such as cast kotlin/ranges/IntRange to kotlin/ranges/IntProgression, should be allowed
        )
            value
        else
            super.unaryOperation(insn, value)
    }

    protected open fun isExactValue(value: BasicValue) =
        value is ProgressionIteratorBasicValue ||
                value is CleanBoxedValue ||
                value.type != null && isProgressionClass(value.type)

    private fun isCastToProgression(insn: AbstractInsnNode): Boolean {
        assert(insn.opcode == Opcodes.CHECKCAST) { "Expected opcode Opcodes.CHECKCAST, but ${insn.opcode} found" }
        val desc = (insn as TypeInsnNode).desc
        return desc in setOf(
            "kotlin/ranges/CharProgression",
            "kotlin/ranges/IntProgression",
            "kotlin/ranges/LongProgression"
        )
    }

    override fun merge(v: BasicValue, w: BasicValue) =
        mergeStackValues(v, w)

    fun mergeLocalVariableValues(v: BasicValue, w: BasicValue) =
        merge(v, w, isLocalVariable = true)

    fun mergeStackValues(v: BasicValue, w: BasicValue) =
        merge(v, w, isLocalVariable = false)

    private fun merge(v: BasicValue, w: BasicValue, isLocalVariable: Boolean) =
        when {
            v === StrictBasicValue.UNINITIALIZED_VALUE || w === StrictBasicValue.UNINITIALIZED_VALUE ->
                StrictBasicValue.UNINITIALIZED_VALUE
            v is BoxedBasicValue -> {
                if (w is BoxedBasicValue) {
                    onMergeSuccess(v, w)
                    when {
                        v is TaintedBoxedValue -> v
                        w is TaintedBoxedValue -> w
                        v.type != w.type -> mergeBoxedHazardous(v, w, isLocalVariable)
                        else -> v // two clean boxed values with the same type are equal
                    }
                } else {
                    mergeBoxedHazardous(v, w, isLocalVariable)
                }
            }

            w is BoxedBasicValue ->
                mergeBoxedHazardous(w, v, isLocalVariable)
            else ->
                super.merge(v, w)
        }

    private fun mergeBoxedHazardous(boxed: BoxedBasicValue, other: BasicValue, isLocalVariable: Boolean): BasicValue {
        if (isLocalVariable) {
            return boxed.taint()
        }

        // If we merge a boxed stack value with a value of a different type, mark it as merge hazard immediately:
        // its intended boxed use might be dead code (KT-49092), in which case boxing elimination would produce incompatible stacks.
        onMergeFail(boxed)
        if (other is BoxedBasicValue) {
            onMergeFail(other)
        }
        return boxed
    }

    protected abstract fun onNewBoxedValue(value: BoxedBasicValue)
    protected abstract fun onUnboxing(insn: AbstractInsnNode, value: BoxedBasicValue, resultType: Type)
    protected abstract fun onAreEqual(insn: AbstractInsnNode, value1: BoxedBasicValue, value2: BoxedBasicValue)
    protected abstract fun onCompareTo(insn: AbstractInsnNode, value1: BoxedBasicValue, value2: BoxedBasicValue)
    protected abstract fun onMethodCallWithBoxedValue(value: BoxedBasicValue)
    protected abstract fun onMergeFail(value: BoxedBasicValue)
    protected abstract fun onMergeSuccess(v: BoxedBasicValue, w: BoxedBasicValue)

}

private val UNBOXING_METHOD_NAMES =
    ImmutableSet.of("booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue")

private val KCLASS_TO_JLCLASS = Type.getMethodDescriptor(AsmTypes.JAVA_CLASS_TYPE, AsmTypes.K_CLASS_TYPE)
private val JLCLASS_TO_KCLASS = Type.getMethodDescriptor(AsmTypes.K_CLASS_TYPE, AsmTypes.JAVA_CLASS_TYPE)

fun AbstractInsnNode.isUnboxing(state: GenerationState) =
    isPrimitiveUnboxing() || isJavaLangClassUnboxing() || isInlineClassUnboxing(state) || isMultiFieldValueClassUnboxing(state)

fun AbstractInsnNode.isBoxing(state: GenerationState) =
    isPrimitiveBoxing() || isJavaLangClassBoxing() || isInlineClassBoxing(state) || isCoroutinePrimitiveBoxing() || isMultiFieldValueClassBoxing(state)

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
    JvmPrimitiveType.isWrapperClassInternalName(internalClassName)

private fun isUnboxingMethodName(name: String) =
    UNBOXING_METHOD_NAMES.contains(name)

fun AbstractInsnNode.isPrimitiveBoxing() =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        isWrapperClassName(owner) &&
                name == "valueOf" &&
                isBoxingMethodDescriptor()
    }

private val BOXING_CLASS_INTERNAL_NAME =
    StandardNames.COROUTINES_JVM_INTERNAL_PACKAGE_FQ_NAME.child(Name.identifier("Boxing")).topLevelClassInternalName()

private fun isJvmPrimitiveName(name: String) = JvmPrimitiveType.values().any { it.javaKeywordName == name }

fun AbstractInsnNode.isCoroutinePrimitiveBoxing(): Boolean {
    return isMethodInsnWith(Opcodes.INVOKESTATIC) {
        owner == BOXING_CLASS_INTERNAL_NAME &&
                name.startsWith("box") &&
                isJvmPrimitiveName(name.substring(3).lowercase())
    }
}

private fun MethodInsnNode.isBoxingMethodDescriptor() =
    JvmPrimitiveType.isBoxingMethodDescriptor(owner, desc)

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

private fun AbstractInsnNode.isMultiFieldValueClassBoxing(state: GenerationState) =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        isMultiFieldValueClassBoxingMethodDescriptor(state)
    }

private fun AbstractInsnNode.isInlineClassUnboxing(state: GenerationState) =
    isMethodInsnWith(Opcodes.INVOKEVIRTUAL) {
        isInlineClassUnboxingMethodDescriptor(state)
    }

private fun AbstractInsnNode.isMultiFieldValueClassUnboxing(state: GenerationState) =
    state.config.supportMultiFieldValueClasses && isMethodInsnWith(Opcodes.INVOKEVIRTUAL) {
        isMultiFieldValueClassUnboxingMethodDescriptor(state)
    }

private fun MethodInsnNode.isInlineClassBoxingMethodDescriptor(state: GenerationState): Boolean {
    if (name != KotlinTypeMapper.BOX_JVM_METHOD_NAME) return false

    val ownerType = Type.getObjectType(owner)
    val unboxedType = unboxedTypeOfInlineClass(ownerType, state) ?: return false
    return desc == Type.getMethodDescriptor(ownerType, unboxedType)
}

private fun MethodInsnNode.isMultiFieldValueClassBoxingMethodDescriptor(state: GenerationState): Boolean {
    if (name != KotlinTypeMapper.BOX_JVM_METHOD_NAME) return false

    val ownerType = Type.getObjectType(owner)
    val multiFieldValueClassUnboxInfo = getMultiFieldValueClassUnboxInfo(ownerType, state) ?: return false
    return desc == Type.getMethodDescriptor(ownerType, *multiFieldValueClassUnboxInfo.unboxedTypes.toTypedArray())
}

private fun MethodInsnNode.isInlineClassUnboxingMethodDescriptor(state: GenerationState): Boolean {
    if (name != KotlinTypeMapper.UNBOX_JVM_METHOD_NAME) return false

    val ownerType = Type.getObjectType(owner)
    val unboxedType = unboxedTypeOfInlineClass(ownerType, state) ?: return false
    return desc == Type.getMethodDescriptor(unboxedType)
}

private fun MethodInsnNode.isMultiFieldValueClassUnboxingMethodDescriptor(state: GenerationState): Boolean {
    val ownerType = Type.getObjectType(owner)
    val multiFieldValueClassUnboxInfo = getMultiFieldValueClassUnboxInfo(ownerType, state) ?: return false
    return multiFieldValueClassUnboxInfo.unboxedTypesAndMethodNamesAndFieldNames.any { (type, methodName) ->
        name == methodName && desc == Type.getMethodDescriptor(type)
    }
}

fun AbstractInsnNode.isNextMethodCallOfProgressionIterator(values: List<BasicValue>) =
    values.firstOrNull() is ProgressionIteratorBasicValue &&
            isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
                name == "next"
            }

fun AbstractInsnNode.isIteratorMethodCall() =
    isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
        name == "iterator" && desc == "()Ljava/util/Iterator;"
    }

fun AbstractInsnNode.isIteratorMethodCallOfProgression(values: List<BasicValue>) =
    isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
        val firstArgType = values.firstOrNull()?.type
        name == "iterator" && desc == "()Ljava/util/Iterator;" &&
                firstArgType != null && isProgressionClass(firstArgType)
    }

private val PROGRESSION_CLASS_FQNS = setOf(
    CHAR_RANGE_FQN, CHAR_PROGRESSION_FQN,
    INT_RANGE_FQN, INT_PROGRESSION_FQN,
    LONG_RANGE_FQN, LONG_PROGRESSION_FQN
)

private fun isProgressionClass(type: Type) =
    type.className in PROGRESSION_CLASS_FQNS


fun AbstractInsnNode.isAreEqualIntrinsicForSameTypedBoxedValues(values: List<BasicValue>) =
    isAreEqualIntrinsic() && areSameTypedPrimitiveBoxedValues(values)

fun areSameTypedPrimitiveBoxedValues(values: List<BasicValue>): Boolean {
    if (values.size != 2) return false
    val (v1, v2) = values
    return v1 is BoxedBasicValue &&
            v2 is BoxedBasicValue &&
            !v1.descriptor.isValueClassValue && !v2.descriptor.isValueClassValue &&
            v1.descriptor.unboxedTypes.single() == v2.descriptor.unboxedTypes.single()
}

fun AbstractInsnNode.isAreEqualIntrinsic() =
    isMethodInsnWith(Opcodes.INVOKESTATIC) {
        name == "areEqual" &&
                owner == IntrinsicMethods.INTRINSICS_CLASS_NAME &&
                desc == "(Ljava/lang/Object;Ljava/lang/Object;)Z"
    }

private val shouldUseEqualsForWrappers = setOf(Type.DOUBLE_TYPE, Type.FLOAT_TYPE, AsmTypes.JAVA_CLASS_TYPE)

fun canValuesBeUnboxedForAreEqual(values: List<BasicValue>, generationState: GenerationState): Boolean = values.none {
    val unboxedType = getUnboxedTypes(it.type, generationState, getMultiFieldValueClassUnboxInfo(it.type, generationState)).singleOrNull()
    unboxedType == null || unboxedType in shouldUseEqualsForWrappers
}

fun AbstractInsnNode.isJavaLangComparableCompareToForSameTypedBoxedValues(values: List<BasicValue>) =
    isJavaLangComparableCompareTo() && areSameTypedPrimitiveBoxedValues(values)

fun AbstractInsnNode.isJavaLangComparableCompareTo() =
    isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
        name == "compareTo" &&
                owner == "java/lang/Comparable" &&
                desc == "(Ljava/lang/Object;)I"
    }
