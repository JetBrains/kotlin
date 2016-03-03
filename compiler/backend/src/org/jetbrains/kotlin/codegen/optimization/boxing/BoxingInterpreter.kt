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

package org.jetbrains.kotlin.codegen.optimization.boxing;

import com.google.common.collect.ImmutableSet
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.RangeCodegenUtil
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmPrimitiveType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import java.util.*

open class BoxingInterpreter(private val insnList: InsnList) : OptimizationBasicInterpreter() {
    private val boxingPlaces = HashMap<Int, BoxedBasicValue>()

    protected open fun createNewBoxing(insn: AbstractInsnNode, type: Type, progressionIterator: ProgressionIteratorBasicValue?): BasicValue {
        val index = insnList.indexOf(insn)
        return boxingPlaces.getOrPut(index) {
            val boxedBasicValue = BoxedBasicValue(type, insn, progressionIterator)
            onNewBoxedValue(boxedBasicValue)
            boxedBasicValue
        }
    }

    @Throws(AnalyzerException::class)
    override fun naryOperation(insn: AbstractInsnNode, values: List<BasicValue>): BasicValue? {
        val value = super.naryOperation(insn, values)
        val firstArg = values.firstOrNull() ?: return value

        return when {
            isBoxing(insn) -> {
                createNewBoxing(insn, value.type, null)
            }
            isUnboxing(insn) && firstArg is BoxedBasicValue -> {
                onUnboxing(insn, firstArg, value.type)
                value
            }
            isIteratorMethodCallOfProgression(insn, values) -> {
                ProgressionIteratorBasicValue(getValuesTypeOfProgressionClass(firstArg.type.internalName))
            }
            isNextMethodCallOfProgressionIterator(insn, values) -> {
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

    @Throws(AnalyzerException::class)
    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue): BasicValue? =
            if (insn.opcode == Opcodes.CHECKCAST && isExactValue(value))
                value
            else
                super.unaryOperation(insn, value)

    protected open fun isExactValue(value: BasicValue) =
            value is ProgressionIteratorBasicValue ||
            value is BoxedBasicValue ||
            value.type != null && isProgressionClass(value.type.internalName)

    override fun merge(v: BasicValue, w: BasicValue) =
            when {
                v == BasicValue.UNINITIALIZED_VALUE || w == BasicValue.UNINITIALIZED_VALUE -> {
                    BasicValue.UNINITIALIZED_VALUE
                }
                v is BoxedBasicValue && v.typeEquals(w) -> {
                    onMergeSuccess(v, w as BoxedBasicValue)
                    v
                }
                else -> {
                    if (v is BoxedBasicValue) {
                        onMergeFail(v)
                    }
                    if (w is BoxedBasicValue) {
                        onMergeFail(w)
                    }
                    super.merge(v, w)
                }
            }

    protected open fun onNewBoxedValue(value: BoxedBasicValue) {}
    protected open fun onUnboxing(insn: AbstractInsnNode, value: BoxedBasicValue, resultType: Type) {}
    protected open fun onMethodCallWithBoxedValue(value: BoxedBasicValue) {}
    protected open fun onMergeFail(value: BoxedBasicValue) {}
    protected open fun onMergeSuccess(v: BoxedBasicValue, w: BoxedBasicValue) {}

    companion object {
        private val UNBOXING_METHOD_NAMES =
                ImmutableSet.of("booleanValue", "charValue", "byteValue", "shortValue", "intValue", "floatValue", "longValue", "doubleValue")

        private fun isWrapperClassNameOrNumber(internalClassName: String) =
                isWrapperClassName(internalClassName) || internalClassName == Type.getInternalName(Number::class.java)

        private fun isWrapperClassName(internalClassName: String) =
                JvmPrimitiveType.isWrapperClassName(buildFqNameByInternal(internalClassName))

        private fun buildFqNameByInternal(internalClassName: String) =
                FqName(Type.getObjectType(internalClassName).className)

        private fun isUnboxing(insn: AbstractInsnNode) =
                insn.opcode == Opcodes.INVOKEVIRTUAL && run {
                    val methodInsn = insn as MethodInsnNode
                    isWrapperClassNameOrNumber(methodInsn.owner) && isUnboxingMethodName(methodInsn.name)
                }

        private fun isUnboxingMethodName(name: String) =
                UNBOXING_METHOD_NAMES.contains(name)

        private fun isBoxing(insn: AbstractInsnNode) =
                insn.opcode == Opcodes.INVOKESTATIC && run {
                    val methodInsn = insn as MethodInsnNode
                    isWrapperClassName(methodInsn.owner) && methodInsn.name == "valueOf" && run {
                        val ownerType = Type.getObjectType(methodInsn.owner)
                        methodInsn.desc == Type.getMethodDescriptor(ownerType, AsmUtil.unboxType(ownerType))
                    }
                }

        private fun isNextMethodCallOfProgressionIterator(insn: AbstractInsnNode, values: kotlin.collections.List<BasicValue>) =
                insn.opcode == Opcodes.INVOKEINTERFACE &&
                values[0] is ProgressionIteratorBasicValue &&
                (insn as MethodInsnNode).name == "next"

        private fun isIteratorMethodCallOfProgression(insn: AbstractInsnNode, values: kotlin.collections.List<BasicValue>) =
                insn.opcode == Opcodes.INVOKEINTERFACE && run {
                    val firstArgType = values[0].type
                    firstArgType != null && isProgressionClass(firstArgType.internalName) && "iterator" == (insn as MethodInsnNode).name
                }

        private fun isProgressionClass(internalClassName: String) =
                RangeCodegenUtil.isRangeOrProgression(buildFqNameByInternal(internalClassName))

        private fun getValuesTypeOfProgressionClass(progressionClassInternalName: String) =
                RangeCodegenUtil.getPrimitiveRangeOrProgressionElementType(buildFqNameByInternal(progressionClassInternalName))?.let { type ->
                    type.typeName.asString()
                } ?: error("type should be not null")
    }
}
