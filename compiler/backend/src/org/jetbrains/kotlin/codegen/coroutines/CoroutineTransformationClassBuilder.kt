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

import com.intellij.util.containers.Stack
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.optimization.DeadCodeEliminationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.FixStackWithLabelNormalizationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.common.StrictBasicValue
import org.jetbrains.kotlin.codegen.optimization.common.analyzeLiveness
import org.jetbrains.kotlin.codegen.optimization.common.insnListOf
import org.jetbrains.kotlin.codegen.optimization.common.removeEmptyCatchBlocks
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*

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

        val customCoroutineStartMarker = methodNode.instructions.toArray().filterIsInstance<MethodInsnNode>().firstOrNull {
            it.owner == COROUTINE_MARKER_OWNER && it.name == ACTUAL_COROUTINE_START_MARKER_NAME
        }

        val customCoroutineStart = customCoroutineStartMarker?.next
        customCoroutineStartMarker?.let(methodNode.instructions::remove)

        val suspensionPoints = collectSuspensionPoints(methodNode)

        for (suspensionPoint in suspensionPoints) {
            splitTryCatchBlocksContainingSuspensionPoint(methodNode, suspensionPoint)
        }

        // Spill stack to variables before suspension points, try/catch blocks
        FixStackWithLabelNormalizationMethodTransformer().transform(classBuilder.thisName, methodNode)

        // Remove unreachable suspension points
        // If we don't do this, then relevant frames will not be analyzed, that is unexpected from point of view of next steps (e.g. variable spilling)
        removeUnreachableSuspensionPointsAndExitPoints(methodNode, suspensionPoints)

        processUninitializedStores(methodNode)

        spillVariables(suspensionPoints, methodNode)

        val suspendMarkerVarIndex = methodNode.maxLocals++

        val suspensionPointLabels = suspensionPoints.withIndex().map {
            transformCallAndReturnContinuationLabel(it.index + 1, it.value, methodNode, suspendMarkerVarIndex)
        }

        methodNode.instructions.apply {
            val startLabel = LabelNode()
            val defaultLabel = LabelNode()
            val firstToInsertBefore = customCoroutineStart ?: first
            // tableswitch(this.label)
            insertBefore(firstToInsertBefore,
                         insnListOf(
                                 *withInstructionAdapter { loadCoroutineSuspendedMarker() }.toArray(),
                                 VarInsnNode(Opcodes.ASTORE, suspendMarkerVarIndex),
                                 VarInsnNode(Opcodes.ALOAD, 0),
                                 FieldInsnNode(
                                         Opcodes.GETFIELD,
                                         COROUTINE_IMPL_ASM_TYPE.internalName,
                                         COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor
                                 ),
                                 TableSwitchInsnNode(0,
                                                     suspensionPoints.size,
                                                     defaultLabel,
                                                     startLabel, *suspensionPointLabels.toTypedArray()
                                 ),
                                 startLabel
                         )
            )

            insert(startLabel, withInstructionAdapter(InstructionAdapter::generateResumeWithExceptionCheck))

            insert(last, withInstructionAdapter {
                visitLabel(defaultLabel.label)
                AsmUtil.genThrow(this, "java/lang/IllegalStateException", "call to 'resume' before 'invoke' with coroutine")
                areturn(Type.VOID_TYPE)
            })
        }

        dropSuspensionMarkers(methodNode, suspensionPoints)
        methodNode.removeEmptyCatchBlocks()

    }

    private fun removeUnreachableSuspensionPointsAndExitPoints(methodNode: MethodNode, suspensionPoints: MutableList<SuspensionPoint>) {
        val dceResult = DeadCodeEliminationMethodTransformer().transformWithResult(classBuilder.thisName, methodNode)

        // If the suspension call begin is alive and suspension call end is dead
        // (e.g., an inlined suspend function call ends with throwing a exception -- see KT-15017),
        // this is an exit point for the corresponding coroutine.
        // It doesn't introduce an additional state to the corresponding coroutine's FSM.
        suspensionPoints.forEach {
            if (dceResult.isAlive(it.suspensionCallBegin) && dceResult.isRemoved(it.suspensionCallEnd)) {
                methodNode.instructions.remove(it.suspensionCallBegin)
            }
        }

        suspensionPoints.removeAll { dceResult.isRemoved(it.suspensionCallBegin) || dceResult.isRemoved(it.suspensionCallEnd) }
    }

    private fun collectSuspensionPoints(methodNode: MethodNode): MutableList<SuspensionPoint> {
        val suspensionPoints = mutableListOf<SuspensionPoint>()
        val beforeSuspensionPointMarkerStack = Stack<MethodInsnNode>()

        for (methodInsn in methodNode.instructions.toArray().filterIsInstance<MethodInsnNode>()) {
            if (methodInsn.owner != COROUTINE_MARKER_OWNER) continue

            when (methodInsn.name) {
                BEFORE_SUSPENSION_POINT_MARKER_NAME -> {
                    beforeSuspensionPointMarkerStack.add(methodInsn)
                }

                AFTER_SUSPENSION_POINT_MARKER_NAME -> {
                    suspensionPoints.add(SuspensionPoint(beforeSuspensionPointMarkerStack.pop(), methodInsn))
                }
            }
        }

        assert(beforeSuspensionPointMarkerStack.isEmpty()) { "Unbalanced suspension markers stack" }

        return suspensionPoints
    }

    private fun dropSuspensionMarkers(methodNode: MethodNode, suspensionPoints: List<SuspensionPoint>) {
        // Drop markers
        suspensionPoints.forEach {
            // before-marker
            methodNode.instructions.remove(it.suspensionCallBegin)
            // after-marker
            methodNode.instructions.remove(it.suspensionCallEnd)
        }
    }

    private fun spillVariables(suspensionPoints: List<SuspensionPoint>, methodNode: MethodNode) {
        val instructions = methodNode.instructions
        val frames = performRefinedTypeAnalysis(methodNode, classBuilder.thisName)
        fun AbstractInsnNode.index() = instructions.indexOf(this)

        // We postpone these actions because they change instruction indices that we use when obtaining frames
        val postponedActions = mutableListOf<() -> Unit>()
        val maxVarsCountByType = mutableMapOf<Type, Int>()
        val livenessFrames = analyzeLiveness(methodNode)

        for (suspension in suspensionPoints) {
            val suspensionCallBegin = suspension.suspensionCallBegin

            assert(frames[suspension.suspensionCallEnd.next.index()]?.stackSize == 1) {
                "Stack should be spilled before suspension call"
            }

            val frame = frames[suspensionCallBegin.index()].sure { "Suspension points containing in dead code must be removed" }
            val localsCount = frame.locals
            val varsCountByType = mutableMapOf<Type, Int>()

            // We consider variable liveness to avoid problems with inline suspension functions:
            // <spill variables>
            // <inline suspension call with new variables initialized> *
            // RETURN (appears only on further transformation phase)
            // ...
            // <spill variables before next suspension point>
            //
            // The problem is that during current phase (before inserting RETURN opcode) we suppose variables generated
            // within inline suspension point as correctly initialized, thus trying to spill them.
            // While after RETURN introduction these variables become uninitialized (at the same time they can't be used further).
            // So we only spill variables that are alive at the begin of suspension point.
            // NB: it's also rather useful for sake of optimization
            val livenessFrame = livenessFrames[suspensionCallBegin.index()]

            // 0 - this
            // 1 - continuation argument
            // 2 - continuation exception
            val variablesToSpill =
                    (3 until localsCount)
                            .map { Pair(it, frame.getLocal(it)) }
                            .filter {
                                val (index, value) = it
                                value != StrictBasicValue.UNINITIALIZED_VALUE && livenessFrame.isAlive(index)
                            }

            for ((index, basicValue) in variablesToSpill) {
                if (basicValue === StrictBasicValue.NULL_VALUE) {
                    postponedActions.add {
                        with(instructions) {
                            insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                                aconst(null)
                                store(index, AsmTypes.OBJECT_TYPE)
                            })
                        }
                    }
                    continue
                }

                val type = basicValue.type
                val normalizedType = type.normalize()

                val indexBySort = varsCountByType[normalizedType]?.plus(1) ?: 0
                varsCountByType[normalizedType] = indexBySort

                val fieldName = normalizedType.fieldNameForVar(indexBySort)

                postponedActions.add {
                    with(instructions) {
                        // store variable before suspension call
                        insertBefore(suspension.suspensionCallBegin, withInstructionAdapter {
                            load(0, AsmTypes.OBJECT_TYPE)
                            load(index, type)
                            StackValue.coerce(type, normalizedType, this)
                            putfield(classBuilder.thisName, fieldName, normalizedType.descriptor)
                        })

                        // restore variable after suspension call
                        insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
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
                        JvmDeclarationOrigin.NO_ORIGIN, Opcodes.ACC_PRIVATE,
                        type.fieldNameForVar(index), type.descriptor, null, null)
            }
        }
    }

    /**
     * See 'splitTryCatchBlocksContainingSuspensionPoint'
     */
    private val SuspensionPoint.tryCatchBlockEndLabelAfterSuspensionCall: LabelNode
        get() {
            assert(suspensionCallEnd.next is LabelNode) {
                "Next instruction after ${this} should be a label, but " +
                "${suspensionCallEnd.next.javaClass}/${suspensionCallEnd.next.opcode} was found"
            }

            return suspensionCallEnd.next as LabelNode
        }

    private fun transformCallAndReturnContinuationLabel(
            id: Int,
            suspension: SuspensionPoint,
            methodNode: MethodNode,
            suspendMarkerVarIndex: Int
    ): LabelNode {
        val continuationLabel = LabelNode()
        val continuationLabelAfterLoadedResult = LabelNode()
        with(methodNode.instructions) {
            // Save state
            insertBefore(suspension.suspensionCallBegin,
                         insnListOf(
                                 VarInsnNode(Opcodes.ALOAD, 0),
                                 *withInstructionAdapter { iconst(id) }.toArray(),
                                 FieldInsnNode(
                                         Opcodes.PUTFIELD, COROUTINE_IMPL_ASM_TYPE.internalName, COROUTINE_LABEL_FIELD_NAME,
                                         Type.INT_TYPE.descriptor
                                 )
                         )
            )

            insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                dup()
                load(suspendMarkerVarIndex, AsmTypes.OBJECT_TYPE)
                ifacmpne(continuationLabelAfterLoadedResult.label)

                // Exit
                load(suspendMarkerVarIndex, AsmTypes.OBJECT_TYPE)
                areturn(AsmTypes.OBJECT_TYPE)
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
                generateResumeWithExceptionCheck()

                // Load continuation argument just like suspending function returns it
                load(1, AsmTypes.OBJECT_TYPE)

                visitLabel(continuationLabelAfterLoadedResult.label)
            })
        }

        return continuationLabel
    }

    // It's necessary to preserve some sensible invariants like there should be no jump in the middle of try-catch-block
    // Also it's important that spilled variables are being restored outside of TCB,
    // otherwise they would be treated as uninitialized within catch-block while they can be used there
    // How suspension point area will look like after all transformations:
    // <spill variables>
    // INVOKESTATIC beforeSuspensionMarker
    // INVOKEVIRTUAL suspensionMethod()Ljava/lang/Object;
    // CHECKCAST SomeType
    // INVOKESTATIC afterSuspensionMarker
    // L1: -- end of all TCB's that are containing the suspension point (inserted by this method)
    // RETURN
    // L2: -- continuation label (used for the TABLESWITCH)
    // <restore variables> (no try-catch blocks here)
    // L3: begin/continuation of all TCB's that are containing the suspension point (inserted by this method)
    // ...
    private fun splitTryCatchBlocksContainingSuspensionPoint(methodNode: MethodNode, suspensionPoint: SuspensionPoint) {
        val instructions = methodNode.instructions
        val beginIndex = instructions.indexOf(suspensionPoint.suspensionCallBegin)
        val endIndex = instructions.indexOf(suspensionPoint.suspensionCallEnd)

        val firstLabel = LabelNode()
        val secondLabel = LabelNode()
        instructions.insert(suspensionPoint.suspensionCallEnd, firstLabel)
        // NOP is needed to preventing these label merge
        // Here between these labels additional instructions are supposed to be inserted (variables spilling, etc.)
        instructions.insert(firstLabel, InsnNode(Opcodes.NOP))
        instructions.insert(firstLabel.next, secondLabel)

        methodNode.tryCatchBlocks =
                methodNode.tryCatchBlocks.flatMap {
                    val isContainingSuspensionPoint =
                            instructions.indexOf(it.start) < beginIndex && beginIndex < instructions.indexOf(it.end)

                    if (isContainingSuspensionPoint) {
                        assert(instructions.indexOf(it.start) < endIndex && endIndex < instructions.indexOf(it.end)) {
                            "Try catch block containing marker before suspension point should also contain the marker after suspension point"
                        }
                        listOf(TryCatchBlockNode(it.start, firstLabel, it.handler, it.type),
                               TryCatchBlockNode(secondLabel, it.end, it.handler, it.type))
                    }
                    else
                        listOf(it)
                }

        suspensionPoint.tryCatchBlocksContinuationLabel = secondLabel

        return
    }
}

private fun InstructionAdapter.generateResumeWithExceptionCheck() {
    // Check if resumeWithException has been called
    load(2, AsmTypes.OBJECT_TYPE)
    dup()
    val noExceptionLabel = Label()
    ifnull(noExceptionLabel)
    athrow()

    mark(noExceptionLabel)
    pop()
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

/**
 * Suspension call may consists of several instructions:
 * INVOKESTATIC beforeSuspensionMarker
 * INVOKEVIRTUAL suspensionMethod()Ljava/lang/Object; // actually it could be some inline method instead of plain call
 * CHECKCAST Type
 * INVOKESTATIC afterSuspensionMarker
 */
private class SuspensionPoint(
        // INVOKESTATIC beforeSuspensionMarker
        val suspensionCallBegin: AbstractInsnNode,
        // INVOKESTATIC afterSuspensionMarker
        val suspensionCallEnd: AbstractInsnNode
) {
    lateinit var tryCatchBlocksContinuationLabel: LabelNode
}
