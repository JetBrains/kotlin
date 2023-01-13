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

import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.isNeedClassReificationMarker
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.*
import kotlin.math.max

private data class Condition(
    val mask: Int, val constant: Int,
    val maskInstruction: VarInsnNode,
    val jumpInstruction: JumpInsnNode,
    val varInsNode: VarInsnNode?
) {
    val expandNotDelete = mask and constant != 0
    val varIndex = varInsNode?.`var` ?: 0
}

class ExtractedDefaultLambda(val type: Type, val capturedArgs: Array<Type>, val offset: Int, val needReification: Boolean)

fun expandMaskConditionsAndUpdateVariableNodes(
    node: MethodNode,
    maskStartIndex: Int,
    masks: List<Int>,
    methodHandlerIndex: Int,
    validOffsets: Collection<Int>
): List<ExtractedDefaultLambda> {
    fun isMaskIndex(varIndex: Int): Boolean {
        return maskStartIndex <= varIndex && varIndex < maskStartIndex + masks.size
    }

    val maskProcessingHeader = node.instructions.asSequence().takeWhile {
        if (it is VarInsnNode) {
            if (isMaskIndex(it.`var`)) {
                /*if slot for default mask is updated than we occurred in actual function body*/
                return@takeWhile it.opcode == Opcodes.ILOAD
            } else if (methodHandlerIndex == it.`var`) {
                return@takeWhile it.opcode == Opcodes.ALOAD
            }
        }
        true
    }

    val conditions = maskProcessingHeader.filterIsInstance<VarInsnNode>().mapNotNull {
        if (isMaskIndex(it.`var`) &&
            it.next?.next?.opcode == Opcodes.IAND &&
            it.next.next.next?.opcode == Opcodes.IFEQ
        ) {
            val jumpInstruction = it.next?.next?.next as JumpInsnNode
            Condition(
                masks[it.`var` - maskStartIndex],
                getConstant(it.next),
                it,
                jumpInstruction,
                jumpInstruction.label.previous as VarInsnNode
            )
        } else if (methodHandlerIndex == it.`var` &&
            it.next?.opcode == Opcodes.IFNULL &&
            it.next.next?.opcode == Opcodes.NEW
        ) {
            //Always delete method handle for now
            //This logic should be updated when method handles would be supported
            Condition(0, 0, it, it.next as JumpInsnNode, null)
        } else null
    }.toList()

    val toDelete = linkedSetOf<AbstractInsnNode>()
    val toInsert = arrayListOf<Pair<AbstractInsnNode, AbstractInsnNode>>()

    val extractable = conditions.filter { it.expandNotDelete && it.varIndex in validOffsets }
    val defaultLambdasInfo = extractDefaultLambdasInfo(extractable, toDelete, toInsert)

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

    node.localVariables.removeIf {
        (it.start in toDelete && it.end in toDelete) || validOffsets.contains(it.index)
    }

    node.tryCatchBlocks.removeIf {
        toDelete.contains(it.start) && toDelete.contains(it.end)
    }

    node.remove(toDelete)

    return defaultLambdasInfo
}

private fun extractDefaultLambdasInfo(
    conditions: List<Condition>,
    toDelete: MutableCollection<AbstractInsnNode>,
    toInsert: MutableList<Pair<AbstractInsnNode, AbstractInsnNode>>
): List<ExtractedDefaultLambda> {
    return conditions.map {
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

                val needReification =
                    instanceCreation.previous.takeIf { isNeedClassReificationMarker(it) }?.let { toDelete.add(it) } != null
                Triple(Type.getObjectType(instanceInstuction.owner), Type.getArgumentTypes(instanceInstuction.desc), needReification)
            }

            is FieldInsnNode -> {
                toDelete.addAll(InsnSequence(instanceInstuction, varAssignmentInstruction.next).toList())

                val needReification =
                    instanceInstuction.previous.takeIf { isNeedClassReificationMarker(it) }?.let { toDelete.add(it) } != null

                Triple(Type.getObjectType(instanceInstuction.owner), emptyArray<Type>(), needReification)
            }
            else -> throw RuntimeException("Can't extract default lambda info $it.\n Unknown instruction: ${instanceInstuction.insnText}")
        }

        toInsert.add(varAssignmentInstruction to defaultLambdaFakeCallStub(argTypes, it.varIndex))

        ExtractedDefaultLambda(owner, argTypes, it.varIndex, needReification)
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

fun loadDefaultLambdaBody(classBytes: ByteArray, classType: Type, isPropertyReference: Boolean): SMAPAndMethodNode {
    // In general we can't know what the correct unboxed `invoke` is, and what Kotlin types its arguments have,
    // as the type of this object may be any subtype of the parameter's type. All we know is that Function<N>
    // has to have a `invoke` that takes `Object`s and returns an `Object`; everything else needs to be figured
    // out from its contents. TODO: for > 22 arguments, the only argument is an array. `MethodInliner` can't do that.
    val invokeName = if (isPropertyReference) OperatorNameConventions.GET.asString() else OperatorNameConventions.INVOKE.asString()
    val invokeNode = getMethodNode(classBytes, classType) {
        it.name == invokeName && it.returnType == AsmTypes.OBJECT_TYPE && it.argumentTypes.all { arg -> arg == AsmTypes.OBJECT_TYPE }
    } ?: error("can't find erased invoke '$invokeName(Object...): Object' in default lambda '${classType.internalName}'")
    return if (invokeNode.node.access.and(Opcodes.ACC_BRIDGE) == 0)
        invokeNode
    else
        invokeNode.node.inlineBridge(classBytes, classType)
}

private fun MethodNode.inlineBridge(classBytes: ByteArray, classType: Type): SMAPAndMethodNode {
    // If the erased invoke is a bridge, we need to locate the unboxed invoke and inline it. As mentioned above,
    // we don't know what the Kotlin types of its arguments/returned value are, so we can't generate our own
    // boxing/unboxing code; luckily, the bridge already has that.
    val invokeInsn = instructions.singleOrNull { it is MethodInsnNode && it.owner == classType.internalName } as MethodInsnNode?
        ?: error("no single invoke of method on this in '${name}${desc}' of default lambda '${classType.internalName}'")
    val targetMethod = Method(invokeInsn.name, invokeInsn.desc)
    val target = getMethodNode(classBytes, classType, targetMethod)
        ?: error("can't find non-bridge invoke '$targetMethod' in default lambda '${classType.internalName}")

    // Store unboxed/casted arguments in the correct variable slots
    val targetArgs = targetMethod.argumentTypes
    val targetArgsSize = targetArgs.sumOf { it.size } + if (target.node.access.and(Opcodes.ACC_STATIC) == 0) 1 else 0
    var offset = targetArgsSize
    for (type in targetArgs.reversed()) {
        offset -= type.size
        instructions.insertBefore(invokeInsn, VarInsnNode(type.getOpcode(Opcodes.ISTORE), offset))
    }
    if (target.node.access.and(Opcodes.ACC_STATIC) == 0) {
        instructions.insertBefore(invokeInsn, InsnNode(Opcodes.POP)) // this
    }

    // Remap returns and ranges for arguments' LVT entries
    val invokeLabel = LabelNode()
    val returnLabel = LabelNode()
    instructions.insertBefore(invokeInsn, invokeLabel)
    instructions.insert(invokeInsn, returnLabel)
    for (insn in target.node.instructions) {
        if (insn.opcode in Opcodes.IRETURN..Opcodes.RETURN) {
            target.node.instructions.set(insn, JumpInsnNode(Opcodes.GOTO, returnLabel))
        }
    }
    for (local in target.node.localVariables) {
        if (local.index < targetArgsSize) {
            local.start = invokeLabel
            local.end = returnLabel
        }
    }

    // Insert contents of the method into the bridge
    instructions.filterIsInstance<LineNumberNode>().forEach { instructions.remove(it) } // those are not meaningful
    instructions.insertBefore(invokeInsn, target.node.instructions)
    instructions.remove(invokeInsn)
    localVariables = target.node.localVariables
    tryCatchBlocks = target.node.tryCatchBlocks
    maxLocals = max(maxLocals, target.node.maxLocals)
    maxStack = max(maxStack, target.node.maxStack)
    return SMAPAndMethodNode(this, target.classSMAP)
}
