/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.backend.jvm.codegen.getMemberOwnerKind
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.DEFAULT_LAMBDA_FAKE_CALL
import org.jetbrains.kotlin.codegen.inline.InlineCodegenUtil.getConstant
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.isNeedClassReificationMarker
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.insnText
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*

private data class Condition(
        val mask: Int, val constant: Int,
        val maskInstruction: VarInsnNode,
        val jumpInstruction: JumpInsnNode,
        val varInsNode: VarInsnNode?
) {
    val expandNotDelete = mask and constant != 0
    val varIndex = varInsNode?.`var` ?: 0
}

fun extractDefaultLambdaOffsetAndDescriptor(jvmSignature: JvmMethodSignature, functionDescriptor: FunctionDescriptor): Map<Int, ValueParameterDescriptor> {
    val valueParameters = jvmSignature.valueParameters
    val containingDeclaration = functionDescriptor.containingDeclaration
    val kind = containingDeclaration.getMemberOwnerKind().let {
        if (DescriptorUtils.isInterface(containingDeclaration)) {
            OwnerKind.DEFAULT_IMPLS
        }
        else it
    }
    val parameterOffsets = parameterOffsets(AsmUtil.isStaticMethod(kind, functionDescriptor), valueParameters)
    val valueParameterOffset = valueParameters.takeWhile { it.kind != JvmMethodParameterKind.VALUE }.size

    return functionDescriptor.valueParameters.filter {
        InlineUtil.isInlineLambdaParameter(it) && it.declaresDefaultValue()
    }.associateBy {
        parameterOffsets[valueParameterOffset + it.index]
    }
}


fun expandMaskConditionsAndUpdateVariableNodes(
        node: MethodNode,
        maskStartIndex: Int,
        masks: List<Int>,
        methodHandlerIndex: Int,
        defaultLambdas: Map<Int, ValueParameterDescriptor>
): List<DefaultLambda> {
    fun isMaskIndex(varIndex: Int): Boolean {
        return maskStartIndex <= varIndex && varIndex < maskStartIndex + masks.size
    }

    val maskProcessingHeader = node.instructions.asSequence().takeWhile {
        if (it is VarInsnNode) {
            if (isMaskIndex(it.`var`)) {
                /*if slot for default mask is updated than we occurred in actual function body*/
                return@takeWhile it.opcode == Opcodes.ILOAD
            }
            else if (methodHandlerIndex == it.`var`) {
                return@takeWhile it.opcode == Opcodes.ALOAD
            }
        }
        true
    }

    val conditions = maskProcessingHeader.filterIsInstance<VarInsnNode>().mapNotNull {
        if (isMaskIndex(it.`var`) &&
            it.next?.next?.opcode == Opcodes.IAND &&
            it.next.next.next?.opcode == Opcodes.IFEQ) {
            val jumpInstruction = it.next?.next?.next as JumpInsnNode
            Condition(
                    masks[it.`var` - maskStartIndex],
                    getConstant(it.next),
                    it,
                    jumpInstruction,
                    jumpInstruction.label.previous as VarInsnNode
            )
        }
        else if (methodHandlerIndex == it.`var` &&
                 it.next?.opcode == Opcodes.IFNULL &&
                 it.next.next?.opcode == Opcodes.NEW) {
            //Always delete method handle for now
            //This logic should be updated when method handles would be supported
            Condition(0, 0, it, it.next as JumpInsnNode, null)
        }
        else null
    }.toList()

    val toDelete = arrayListOf<AbstractInsnNode>()
    val toInsert = arrayListOf<Pair<AbstractInsnNode, AbstractInsnNode>>()

    val defaultLambdasInfo = extractDefaultLambdasInfo(conditions, defaultLambdas, toDelete, toInsert)

    val indexToVarNode = node.localVariables?.filter { it.index < maskStartIndex }?.associateBy { it.index } ?: emptyMap()
    conditions.forEach {
        val jumpInstruction = it.jumpInstruction
        InsnSequence(it.maskInstruction, (if (it.expandNotDelete) jumpInstruction.next else jumpInstruction.label)).forEach {
            toDelete.add(it)
        }
        if (it.expandNotDelete) {
            indexToVarNode[it.varIndex]?.let { varNode ->
                varNode.start = it.jumpInstruction.label
            }
        }
    }

    toInsert.forEach { (position, newInsn) ->
        node.instructions.insert(position, newInsn)
    }

    node.remove(toDelete)

    return defaultLambdasInfo
}


private fun extractDefaultLambdasInfo(
        conditions: List<Condition>,
        defaultLambdas: Map<Int, ValueParameterDescriptor>,
        toDelete: MutableList<AbstractInsnNode>,
        toInsert: MutableList<Pair<AbstractInsnNode, AbstractInsnNode>>
): List<DefaultLambda> {
    val defaultLambdaConditions = conditions.filter { it.expandNotDelete && defaultLambdas.contains(it.varIndex) }
    return defaultLambdaConditions.map {
        val varAssignmentInstruction = it.varInsNode!!
        var instanceInstuction = varAssignmentInstruction.previous
        if (instanceInstuction is TypeInsnNode && instanceInstuction.opcode == Opcodes.CHECKCAST) {
            instanceInstuction = instanceInstuction.previous
        }

        val (owner, argTypes, needReification) = when (instanceInstuction) {
            is MethodInsnNode -> {
                assert(instanceInstuction.name == "<init>") { "Expected constructor call for default lambda, but $instanceInstuction" }
                val ownerInternalName = instanceInstuction.owner
                val instanceCreation = InsnSequence(it.jumpInstruction, it.jumpInstruction.label).filter {
                    it.opcode == Opcodes.NEW && (it as TypeInsnNode).desc == ownerInternalName
                }.single()

                assert(instanceCreation.next?.opcode == Opcodes.DUP) {
                    "Dup should follow default lambda instanceInstruction creation but ${instanceCreation.next}"
                }

                toDelete.apply {
                    addAll(listOf(instanceCreation, instanceCreation.next))
                    addAll(InsnSequence(instanceInstuction, varAssignmentInstruction.next).toList())
                }

                val needReification = instanceCreation.previous.takeIf { isNeedClassReificationMarker(it) }?.let { toDelete.add(it) } != null
                Triple(Type.getObjectType(instanceInstuction.owner), Type.getArgumentTypes(instanceInstuction.desc), needReification)
            }

            is FieldInsnNode -> {
                toDelete.addAll(InsnSequence(instanceInstuction, varAssignmentInstruction.next).toList())

                val needReification = instanceInstuction.previous.takeIf { isNeedClassReificationMarker(it) }?.let { toDelete.add(it) } != null

                Triple(Type.getObjectType(instanceInstuction.owner), emptyArray<Type>(), needReification)
            }
            else -> throw RuntimeException("Can't extract default lambda info $it.\n Unknown instruction: ${instanceInstuction.insnText}")
        }

        toInsert.add(varAssignmentInstruction to defaultLambdaFakeCallStub(argTypes, it.varIndex))

        DefaultLambda(owner, argTypes, defaultLambdas[it.varIndex]!!, it.varIndex, needReification)
    }
}

//marker that removes captured parameters from stack
//at inlining it would be substituted with parameters store
private fun defaultLambdaFakeCallStub(args: Array<Type>, lambdaOffset: Int): MethodInsnNode {
    return MethodInsnNode(
            Opcodes.INVOKESTATIC,
            DEFAULT_LAMBDA_FAKE_CALL,
            DEFAULT_LAMBDA_FAKE_CALL + lambdaOffset,
            Type.getMethodDescriptor(Type.VOID_TYPE, *args),
            false
    )
}