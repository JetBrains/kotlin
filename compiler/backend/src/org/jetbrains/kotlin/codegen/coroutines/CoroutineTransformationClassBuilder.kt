/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.optimization.DeadCodeEliminationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.MandatoryMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.common.OptimizationBasicInterpreter
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.insnListOf
import org.jetbrains.kotlin.codegen.optimization.common.removeEmptyCatchBlocks
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue

class CoroutineTransformationClassBuilder(private val delegate: ClassBuilder) : DelegatingClassBuilder() {
    override fun getDelegate() = delegate

    override fun newMethod(
            origin: JvmDeclarationOrigin,
            access: Int, name: String,
            desc: String, signature:
            String?,
            exceptions: Array<out String>?
    ) = CoroutineTransformerMethodVisitor(
                delegate.newMethod(origin, access, name, desc, signature, exceptions),
                access, name, desc, signature, exceptions, this)
}

class CoroutineTransformerClassBuilderFactory(delegate: ClassBuilderFactory) : DelegatingClassBuilderFactory(delegate) {
    override fun newClassBuilder(origin: JvmDeclarationOrigin) = CoroutineTransformationClassBuilder(delegate.newClassBuilder(origin))
}

class CoroutineTransformerMethodVisitor(
        delegate: MethodVisitor,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?,
        private val classBuilder: ClassBuilder
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {
    override fun performTransformations(methodNode: MethodNode) {
        if (methodNode.visibleAnnotations?.none { it.desc == CONTINUATION_METHOD_ANNOTATION_DESC } != false) return
        methodNode.visibleAnnotations.removeAll { it.desc == CONTINUATION_METHOD_ANNOTATION_DESC }

        val suspensionPoints = collectSuspensionPoints(methodNode)
        if (suspensionPoints.isEmpty()) return

        for (suspensionPoint in suspensionPoints) {
            splitTryCatchBlocksContainingSuspensionPoint(methodNode, suspensionPoint)
        }

        // Spill stack to variables before suspension points, try/catch blocks
        MandatoryMethodTransformer().transform("fake", methodNode)

        // Remove unreachable suspension points
        // If we don't do this, then relevant frames will not be analyzed, that is unexpected from point of view of next steps (e.g. variable spilling)
        DeadCodeEliminationMethodTransformer().transform("fake", methodNode)
        suspensionPoints.removeAll { it.suspensionCall.next == null && it.suspensionCall.previous == null }

        processUninitializedStores(methodNode)

        spillVariables(suspensionPoints, methodNode)

        val suspensionPointLabels = suspensionPoints.withIndex().map {
            transformCallAndReturnContinuationLabel(it.index + 1, it.value, methodNode)
        }

        methodNode.instructions.apply {
            val startLabel = LabelNode()
            val defaultLabel = LabelNode()
            // tableswitch(this.label)
            insertBefore(first,
                         insnListOf(
                                 VarInsnNode(Opcodes.ALOAD, 0),
                                 FieldInsnNode(
                                         Opcodes.GETFIELD, classBuilder.thisName, COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor),
                                 TableSwitchInsnNode(0,
                                                     suspensionPoints.size,
                                                     defaultLabel,
                                                     *(arrayOf(startLabel) + suspensionPointLabels)),
                                 startLabel))


            insert(last, withInstructionAdapter {
                visitLabel(defaultLabel.label)
                AsmUtil.genThrow(this, "java/lang/IllegalStateException", "call to 'resume' before 'invoke' with coroutine")
                areturn(Type.VOID_TYPE)
            })
        }

        methodNode.removeEmptyCatchBlocks()

    }

    private fun collectSuspensionPoints(methodNode: MethodNode): MutableList<SuspensionPoint> {
        val suspensionPoints = mutableListOf<SuspensionPoint>()

        for (methodInsn in methodNode.instructions.asSequence().filterIsInstance<MethodInsnNode>()) {
            if (methodInsn.owner != SUSPENSION_POINT_MARKER_OWNER) continue

            when (methodInsn.name) {
                SUSPENSION_POINT_MARKER_NAME -> {
                    assert(methodInsn.next is MethodInsnNode) {
                        "Expected method call instruction after suspension point, but ${methodInsn.next} found"
                    }

                    suspensionPoints.add(SuspensionPoint(methodInsn.next as MethodInsnNode))
                }

                else -> error("Unexpected suspension point marker kind '${methodInsn.name}'")
            }
        }

        // Drop markers
        suspensionPoints.forEach { methodNode.instructions.remove(it.suspensionCall.previous) }

        return suspensionPoints
    }

    private fun spillVariables(suspensionPoints: List<SuspensionPoint>, methodNode: MethodNode) {
        val instructions = methodNode.instructions
        val frames = MethodTransformer.analyze("fake", methodNode, OptimizationBasicInterpreter())
        fun AbstractInsnNode.index() = instructions.indexOf(this)

        // We postpone these actions because they change instruction indices that we use when obtaining frames
        val postponedActions = mutableListOf<() -> Unit>()
        val maxVarsCountByType = mutableMapOf<Type, Int>()

        for (suspension in suspensionPoints) {
            val call = suspension.suspensionCall
            assert(frames[call.next.index()].stackSize == (if (Type.getReturnType(call.desc).sort == Type.VOID) 0 else 1)) {
                "Stack should be spilled before suspension call"
            }

            val frame = frames[call.index()]
            val localsCount = frame.locals
            val varsCountByType = mutableMapOf<Type, Int>()
            // 0 - this
            // 1 - continuation argument
            // 2 - continuation exception
            val variablesToSpill =
                    (3 until localsCount).map { Pair(it, frame.getLocal(it)) }.filter { it.second != BasicValue.UNINITIALIZED_VALUE }

            for ((index, basicValue) in variablesToSpill) {
                val type = basicValue.type
                val normalizedType = type.normalize()

                val indexBySort = varsCountByType[normalizedType]?.plus(1) ?: 0
                varsCountByType[normalizedType] = indexBySort

                val fieldName = normalizedType.fieldNameForVar(indexBySort)

                postponedActions.add {
                    with(instructions) {
                        // store variable before suspension call
                        insertBefore(call, withInstructionAdapter {
                            load(0, AsmTypes.OBJECT_TYPE)
                            load(index, type)
                            StackValue.coerce(type, normalizedType, this)
                            putfield(classBuilder.thisName, fieldName, normalizedType.descriptor)
                        })

                        // restore variable after suspension call
                        insert(call.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                            load(0, AsmTypes.OBJECT_TYPE)
                            getfield(classBuilder.thisName, fieldName, normalizedType.descriptor)
                            StackValue.coerce(normalizedType, type, this)
                            store(index, type)
                        })
                    }
                }
            }

            varsCountByType.forEach {
                maxVarsCountByType[it.key] = Math.max(maxVarsCountByType[it.key] ?: 0, it.value)
            }
        }

        postponedActions.forEach(Function0<Unit>::invoke)

        maxVarsCountByType.forEach { entry ->
            val (type, maxIndex) = entry
            for (index in 0..maxIndex) {
                classBuilder.newField(
                        JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE or Opcodes.ACC_VOLATILE,
                        type.fieldNameForVar(index), type.descriptor, null, null)
            }
        }
    }

    private val MethodInsnNode.tryCatchBlockEndLabelAfterSuspensionCall: LabelNode
        get() {
            assert(next is LabelNode) {
                "Next instruction after ${this} should be a label, but ${next.javaClass}/${next.opcode} was found"
            }

            return next as LabelNode
        }

    private fun transformCallAndReturnContinuationLabel(id: Int, suspension: SuspensionPoint, methodNode: MethodNode): LabelNode {
        val call = suspension.suspensionCall
        val method = Method(call.name, call.desc)
        call.desc = Method(method.name, Type.VOID_TYPE, method.argumentTypes).descriptor

        val continuationLabel = LabelNode()

        with(methodNode.instructions) {
            // Save state
            insertBefore(call,
                         insnListOf(
                                 VarInsnNode(Opcodes.ALOAD, 0),
                                 *withInstructionAdapter { iconst(id) }.toArray(),
                                 FieldInsnNode(
                                         Opcodes.PUTFIELD, classBuilder.thisName, COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor)))


            insert(call.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                // Exit
                areturn(Type.VOID_TYPE)
                // Mark place for continuation
                visitLabel(continuationLabel.label)
            })

            // After suspension point there is always three nodes: L1, NOP, L2
            // And if there are relevant exception handlers, they always start at L2
            // See 'splitTryCatchBlocksContainingSuspensionPoint'
            val possibleTryCatchBlockStart = suspension.tryCatchBlocksContinuationLabel

            // Remove NOP as it's unnecessary anymore
            assert(possibleTryCatchBlockStart.previous.opcode == Opcodes.NOP) {
                "NOP expected but ${possibleTryCatchBlockStart.previous.opcode} was found"
            }
            remove(possibleTryCatchBlockStart.previous)


            insert(possibleTryCatchBlockStart, withInstructionAdapter {
                // Check if resumeWithException has been called
                load(2, AsmTypes.OBJECT_TYPE)
                dup()
                val noExceptionLabel = Label()
                ifnull(noExceptionLabel)
                athrow()

                mark(noExceptionLabel)
                pop()

                // Load continuation argument just like suspending function returns it
                load(1, AsmTypes.OBJECT_TYPE)
                StackValue.coerce(AsmTypes.OBJECT_TYPE, method.returnType, this)
            })
        }

        return continuationLabel
    }

    // It's necessary to preserve some sensible invariants like there should be no jump in the middle of try-catch-block
    // Also it's important that spilled variables are being restored outside of TCB,
    // otherwise they would be treated as uninitialized within catch-block while they can be used there
    // How suspension point area will look like after all transformations:
    // <spill variables>
    // INVOKEVIRTUAL suspensionMethod()
    // L1: -- end of all TCB's that are containing the suspension point (inserted by this method)
    // RETURN
    // L2: -- continuation label (used for the TABLESWITCH)
    // <restore variables> (no try-catch blocks here)
    // L3: begin/continuation of all TCB's that are containing the suspension point (inserted by this method)
    // ...
    private fun splitTryCatchBlocksContainingSuspensionPoint(methodNode: MethodNode, suspensionPoint: SuspensionPoint) {
        val instructions = methodNode.instructions
        val indexOfSuspension = instructions.indexOf(suspensionPoint.suspensionCall)

        val firstLabel = LabelNode()
        val secondLabel = LabelNode()
        instructions.insert(suspensionPoint.suspensionCall, firstLabel)
        // NOP is needed to preventing these label merge
        // Here between these labels additional instructions are supposed to be inserted (variables spilling, etc.)
        instructions.insert(firstLabel, InsnNode(Opcodes.NOP))
        instructions.insert(firstLabel.next, secondLabel)

        methodNode.tryCatchBlocks =
                methodNode.tryCatchBlocks.flatMap {
                    val isContainingSuspensionPoint =
                            instructions.indexOf(it.start) < indexOfSuspension && indexOfSuspension < instructions.indexOf(it.end)

                    if (isContainingSuspensionPoint)
                        listOf(TryCatchBlockNode(it.start, firstLabel, it.handler, it.type),
                               TryCatchBlockNode(secondLabel, it.end, it.handler, it.type))
                    else
                        listOf(it)
                }

        suspensionPoint.tryCatchBlocksContinuationLabel = secondLabel

        return
    }
}

private fun Type.fieldNameForVar(index: Int) = descriptor.first() + "$" + index

private fun withInstructionAdapter(block: InstructionAdapter.() -> Unit): InsnList {
    val tmpMethodNode = MethodNode()

    InstructionAdapter(tmpMethodNode).apply(block)

    return tmpMethodNode.instructions
}

private fun Type.normalize() =
    when (sort) {
        Type.ARRAY, Type.OBJECT -> AsmTypes.OBJECT_TYPE
        else -> this
    }

private class SuspensionPoint(val suspensionCall: MethodInsnNode) {
    lateinit var tryCatchBlocksContinuationLabel: LabelNode
}
