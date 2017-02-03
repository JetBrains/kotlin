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
import org.jetbrains.kotlin.codegen.RangeCodegenUtil
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import java.util.*

open class BoxingInterpreter(private val insnList: InsnList) : OptimizationBasicInterpreter() {
    private val boxingPlaces = HashMap<Int, BoxedBasicValue>()

    protected open fun createNewBoxing(insn: AbstractInsnNode, type: Type, progressionIterator: ProgressionIteratorBasicValue?): BasicValue {
        val index = insnList.indexOf(insn)
        return boxingPlaces.getOrPut(index) {
            val boxedBasicValue = CleanBoxedValue(type, insn, progressionIterator)
            onNewBoxedValue(boxedBasicValue)
            boxedBasicValue
        }
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
            insn.isBoxing() -> {
                createNewBoxing(insn, value.type, null)
            }
            insn.isUnboxing() && firstArg is BoxedBasicValue -> {
                onUnboxing(insn, firstArg, value.type)
                value
            }
            insn.isIteratorMethodCallOfProgression(values) -> {
                ProgressionIteratorBasicValue(getValuesTypeOfProgressionClass(firstArg.type.internalName))
            }
            insn.isNextMethodCallOfProgressionIterator(values) -> {
                val progressionIterator = firstArg as? ProgressionIteratorBasicValue
                                          ?: throw AssertionError("firstArg should be progression iterator")
                createNewBoxing(insn, AsmUtil.boxType(progressionIterator.valuesPrimitiveType), progressionIterator)
            }
            else -> {
                // N-ary operation should be a method call or multinewarray.
                // Arguments for multinewarray could be only numeric,
                // so if there are boxed values in args, it's not a case of multinewarray.
                for (arg in values) {
                    if (arg is BoxedBasicValue) {
                        onMethodCallWithBoxedValue(arg)
                    }
                }
                value
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
            value.type != null && isProgressionClass(value.type.internalName)

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
    protected open fun onMethodCallWithBoxedValue(value: BoxedBasicValue) {}
    protected open fun onMergeFail(value: BoxedBasicValue) {}
    protected open fun onMergeSuccess(v: BoxedBasicValue, w: BoxedBasicValue) {}

}

private val UNBOXING_METHOD_NAMES =
        ImmutableSet.of("booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue")

private val KCLASS_TO_JLCLASS = Type.getMethodDescriptor(AsmTypes.JAVA_CLASS_TYPE, AsmTypes.K_CLASS_TYPE)
private val JLCLASS_TO_KCLASS = Type.getMethodDescriptor(AsmTypes.K_CLASS_TYPE, AsmTypes.JAVA_CLASS_TYPE)

fun AbstractInsnNode.isUnboxing() =
        isPrimitiveUnboxing() || isJavaLangClassUnboxing()

fun AbstractInsnNode.isBoxing() =
        isPrimitiveBoxing() || isJavaLangClassBoxing()

fun AbstractInsnNode.isPrimitiveUnboxing() =
        isMethodInsnWith(Opcodes.INVOKEVIRTUAL) {
            isWrapperClassNameOrNumber(owner) && isUnboxingMethodName(name)
        }

private fun AbstractInsnNode.isJavaLangClassUnboxing() =
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

private fun AbstractInsnNode.isJavaLangClassBoxing() =
        isMethodInsnWith(Opcodes.INVOKESTATIC) {
            owner == AsmTypes.REFLECTION &&
            name == "getOrCreateKotlinClass" &&
            desc == JLCLASS_TO_KCLASS
        }

private fun AbstractInsnNode.isNextMethodCallOfProgressionIterator(values: List<BasicValue>) =
        values[0] is ProgressionIteratorBasicValue &&
        isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
            name == "next"
        }

private fun AbstractInsnNode.isIteratorMethodCallOfProgression(values: List<BasicValue>) =
        isMethodInsnWith(Opcodes.INVOKEINTERFACE) {
            val firstArgType = values[0].type
            firstArgType != null &&
            isProgressionClass(firstArgType.internalName) &&
            name == "iterator"
        }

private fun isProgressionClass(internalClassName: String) =
        RangeCodegenUtil.isRangeOrProgression(buildFqNameByInternal(internalClassName))

private fun getValuesTypeOfProgressionClass(progressionClassInternalName: String) =
        RangeCodegenUtil.getPrimitiveRangeOrProgressionElementType(buildFqNameByInternal(progressionClassInternalName))
                ?.typeName?.asString() ?: error("type should be not null")
