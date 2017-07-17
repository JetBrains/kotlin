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
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.TransformationMethodVisitor
import org.jetbrains.kotlin.codegen.inline.MaxStackFrameSizeAndLocalsCalculator
import org.jetbrains.kotlin.codegen.inline.isAfterSuspendMarker
import org.jetbrains.kotlin.codegen.inline.isBeforeSuspendMarker
import org.jetbrains.kotlin.codegen.inline.isInlineMarker
import org.jetbrains.kotlin.codegen.optimization.DeadCodeEliminationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.common.*
import org.jetbrains.kotlin.codegen.optimization.fixStack.FixStackMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceValue

class CoroutineTransformerMethodVisitor(
        delegate: MethodVisitor,
        access: Int,
        name: String,
        desc: String,
        signature: String?,
        exceptions: Array<out String>?,
        private val containingClassInternalName: String,
        obtainClassBuilderForCoroutineState: () -> ClassBuilder,
        private val isForNamedFunction: Boolean,
        private val element: KtElement,
        // It's only matters for named functions, may differ from '!isStatic(access)' in case of DefaultImpls
        private val needDispatchReceiver: Boolean = false,
        // May differ from containingClassInternalName in case of DefaultImpls
        private val internalNameForDispatchReceiver: String? = null
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {

    private val classBuilderForCoroutineState: ClassBuilder by lazy(obtainClassBuilderForCoroutineState)

    private val continuationIndex = if (isForNamedFunction) getLastParameterIndex(desc, access) else 0
    private var dataIndex = if (isForNamedFunction) -1 else 1
    private var exceptionIndex = if (isForNamedFunction) -1 else 2

    override fun performTransformations(methodNode: MethodNode) {
        val suspensionPoints = collectSuspensionPoints(methodNode)

        // First instruction in the method node may change in case of named function
        val actualCoroutineStart = methodNode.instructions.first

        FixStackMethodTransformer().transform(containingClassInternalName, methodNode)

        if (isForNamedFunction) {
            if (allSuspensionPointsAreTailCalls(containingClassInternalName, methodNode, suspensionPoints)) {
                dropSuspensionMarkers(methodNode, suspensionPoints)
                return
            }

            dataIndex = methodNode.maxLocals++
            exceptionIndex = methodNode.maxLocals++

            prepareMethodNodePreludeForNamedFunction(methodNode)
        }

        for (suspensionPoint in suspensionPoints) {
            splitTryCatchBlocksContainingSuspensionPoint(methodNode, suspensionPoint)
        }

        // Actual max stack might be increased during the previous phases
        updateMaxStack(methodNode)

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
            val firstToInsertBefore = actualCoroutineStart
            val tableSwitchLabel = LabelNode()
            val lineNumber = CodegenUtil.getLineNumberForElement(element, false) ?: 0

            // tableswitch(this.label)
            insertBefore(firstToInsertBefore,
                         insnListOf(
                                 *withInstructionAdapter { loadCoroutineSuspendedMarker() }.toArray(),
                                 tableSwitchLabel,
                                 // Allow debugger to stop on enter into suspend function
                                 LineNumberNode(lineNumber, tableSwitchLabel),
                                 VarInsnNode(Opcodes.ASTORE, suspendMarkerVarIndex),
                                 VarInsnNode(Opcodes.ALOAD, continuationIndex),
                                 createInsnForReadingLabel(),
                                 TableSwitchInsnNode(0,
                                                     suspensionPoints.size,
                                                     defaultLabel,
                                                     startLabel, *suspensionPointLabels.toTypedArray()
                                 ),
                                 startLabel
                         )
            )

            insert(startLabel, withInstructionAdapter { generateResumeWithExceptionCheck(exceptionIndex) })
            insert(last, defaultLabel)

            insert(last, withInstructionAdapter {
                AsmUtil.genThrow(this, "java/lang/IllegalStateException", "call to 'resume' before 'invoke' with coroutine")
                areturn(Type.VOID_TYPE)
            })
        }

        dropSuspensionMarkers(methodNode, suspensionPoints)
        methodNode.removeEmptyCatchBlocks()
    }

    private fun createInsnForReadingLabel() =
            if (isForNamedFunction)
                MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        classBuilderForCoroutineState.thisName,
                        "getLabel",
                        Type.getMethodDescriptor(Type.INT_TYPE),
                        false
                )
            else
                FieldInsnNode(
                    Opcodes.GETFIELD,
                    COROUTINE_IMPL_ASM_TYPE.internalName,
                    COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor
                )

    private fun createInsnForSettingLabel() =
            if (isForNamedFunction)
                MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        classBuilderForCoroutineState.thisName,
                        "setLabel",
                        Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
                        false
                )
            else
                FieldInsnNode(
                        Opcodes.PUTFIELD,
                        COROUTINE_IMPL_ASM_TYPE.internalName,
                        COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor
                )

    private fun updateMaxStack(methodNode: MethodNode) {
        methodNode.instructions.resetLabels()
        methodNode.accept(
                MaxStackFrameSizeAndLocalsCalculator(
                        Opcodes.ASM5, methodNode.access, methodNode.desc,
                        object : MethodVisitor(Opcodes.ASM5) {
                            override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                                methodNode.maxStack = maxStack
                            }
                        }
                )
        )
    }

    private fun prepareMethodNodePreludeForNamedFunction(methodNode: MethodNode) {
        val objectTypeForState = Type.getObjectType(classBuilderForCoroutineState.thisName)
        methodNode.instructions.insert(withInstructionAdapter {
            val createStateInstance = Label()
            val afterCoroutineStateCreated = Label()

            // We have to distinguish the following situations:
            // - Our function got called in a common way (e.g. from another function or via recursive call) and we should execute our
            // code from the beginning
            // - We got called from `doResume` of our continuation, i.e. we need to continue from the last suspension point
            //
            // Also in the first case we wrap the completion into a special anonymous class instance (let's call it X$1)
            // that we'll use as a continuation argument for suspension points
            //
            // How we distinguish the cases:
            // - If the continuation is not an instance of X$1 we know exactly it's not the second case, because when resuming
            // the continuation we pass an instance of that class
            // - Otherwise it's still can be a recursive call. To check it's not the case we set the last bit in the label in
            // `doResume` just before calling the suspend function (see kotlin.coroutines.experimental.jvm.internal.CoroutineImplForNamedFunction).
            // So, if it's set we're in continuation.
            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            instanceOf(objectTypeForState)
            ifeq(createStateInstance)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            checkcast(objectTypeForState)
            visitVarInsn(Opcodes.ASTORE, continuationIndex)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            invokevirtual(
                    classBuilderForCoroutineState.thisName,
                    "getLabel",
                    Type.getMethodDescriptor(Type.INT_TYPE),
                    false
            )

            iconst(1 shl 31)
            and(Type.INT_TYPE)
            ifeq(createStateInstance)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            dup()
            invokevirtual(
                    classBuilderForCoroutineState.thisName,
                    "getLabel",
                    Type.getMethodDescriptor(Type.INT_TYPE),
                    false
            )

            iconst(1 shl 31)
            sub(Type.INT_TYPE)
            invokevirtual(
                    classBuilderForCoroutineState.thisName,
                    "setLabel",
                    Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
                    false
            )

            goTo(afterCoroutineStateCreated)

            visitLabel(createStateInstance)

            anew(objectTypeForState)
            dup()

            val parameterTypesAndIndices =
                    getParameterTypesIndicesForCoroutineConstructor(
                            methodNode.desc,
                            methodNode.access,
                            needDispatchReceiver, internalNameForDispatchReceiver ?: containingClassInternalName
                    )
            for ((type, index) in parameterTypesAndIndices) {
                load(index, type)
            }

            invokespecial(
                    classBuilderForCoroutineState.thisName,
                    "<init>",
                    Type.getMethodDescriptor(
                            Type.VOID_TYPE,
                            *getParameterTypesForCoroutineConstructor(
                                    methodNode.desc, needDispatchReceiver,
                                    internalNameForDispatchReceiver ?: containingClassInternalName
                            )
                    ),
                    false
            )

            visitVarInsn(Opcodes.ASTORE, continuationIndex)

            visitLabel(afterCoroutineStateCreated)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            getfield(classBuilderForCoroutineState.thisName, DATA_FIELD_NAME, AsmTypes.OBJECT_TYPE.descriptor)
            visitVarInsn(Opcodes.ASTORE, dataIndex)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            getfield(classBuilderForCoroutineState.thisName, EXCEPTION_FIELD_NAME, AsmTypes.JAVA_THROWABLE_TYPE.descriptor)
            visitVarInsn(Opcodes.ASTORE, exceptionIndex)
        })
    }

    private fun removeUnreachableSuspensionPointsAndExitPoints(methodNode: MethodNode, suspensionPoints: MutableList<SuspensionPoint>) {
        val dceResult = DeadCodeEliminationMethodTransformer().transformWithResult(containingClassInternalName, methodNode)

        // If the suspension call begin is alive and suspension call end is dead
        // (e.g., an inlined suspend function call ends with throwing a exception -- see KT-15017),
        // this is an exit point for the corresponding coroutine.
        // It doesn't introduce an additional state to the corresponding coroutine's FSM.
        suspensionPoints.forEach {
            if (dceResult.isAlive(it.suspensionCallBegin) && dceResult.isRemoved(it.suspensionCallEnd)) {
                it.removeBeforeSuspendMarker(methodNode)
            }
        }

        suspensionPoints.removeAll { dceResult.isRemoved(it.suspensionCallBegin) || dceResult.isRemoved(it.suspensionCallEnd) }
    }

    private fun collectSuspensionPoints(methodNode: MethodNode): MutableList<SuspensionPoint> {
        val suspensionPoints = mutableListOf<SuspensionPoint>()
        val beforeSuspensionPointMarkerStack = Stack<AbstractInsnNode>()

        for (methodInsn in methodNode.instructions.toArray().filterIsInstance<MethodInsnNode>()) {
            when {
                isBeforeSuspendMarker(methodInsn) -> {
                    beforeSuspensionPointMarkerStack.add(methodInsn.previous)
                }

                isAfterSuspendMarker(methodInsn) -> {
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
            it.removeBeforeSuspendMarker(methodNode)
            it.removeAfterSuspendMarker(methodNode)
        }
    }

    private fun spillVariables(suspensionPoints: List<SuspensionPoint>, methodNode: MethodNode) {
        val instructions = methodNode.instructions
        val frames = performRefinedTypeAnalysis(methodNode, containingClassInternalName)
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
            // 1 - parameter
            // ...
            // k - continuation
            // k + 1 - data
            // k + 2 - exception
            val variablesToSpill =
                    (0 until localsCount)
                            .filter{ it !in setOf(continuationIndex, dataIndex, exceptionIndex) }
                            .map { Pair(it, frame.getLocal(it)) }
                            .filter { (index, value) ->
                                (index == 0 && needDispatchReceiver && isForNamedFunction) ||
                                (value != StrictBasicValue.UNINITIALIZED_VALUE && livenessFrame.isAlive(index))
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
                            load(continuationIndex, AsmTypes.OBJECT_TYPE)
                            load(index, type)
                            StackValue.coerce(type, normalizedType, this)
                            putfield(classBuilderForCoroutineState.thisName, fieldName, normalizedType.descriptor)
                        })

                        // restore variable after suspension call
                        insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                            load(continuationIndex, AsmTypes.OBJECT_TYPE)
                            getfield(classBuilderForCoroutineState.thisName, fieldName, normalizedType.descriptor)
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
                classBuilderForCoroutineState.newField(
                        JvmDeclarationOrigin.NO_ORIGIN, AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
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
                "${suspensionCallEnd.next::class.java}/${suspensionCallEnd.next.opcode} was found"
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
        val suspendElementLineNumber = CodegenUtil.getLineNumberForElement(element, false) ?: 0
        val nextLineNumberNode = suspension.suspensionCallEnd.findNextOrNull { it is LineNumberNode } as? LineNumberNode
        with(methodNode.instructions) {
            // Save state
            insertBefore(suspension.suspensionCallBegin,
                         insnListOf(
                                 VarInsnNode(Opcodes.ALOAD, continuationIndex),
                                 *withInstructionAdapter { iconst(id) }.toArray(),
                                 createInsnForSettingLabel()
                         )
            )

            insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                dup()
                load(suspendMarkerVarIndex, AsmTypes.OBJECT_TYPE)
                ifacmpne(continuationLabelAfterLoadedResult.label)

                // Exit
                val returnLabel = LabelNode()
                visitLabel(returnLabel.label)
                // Special line number to stop in debugger before suspend return
                visitLineNumber(suspendElementLineNumber, returnLabel.label)
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
                generateResumeWithExceptionCheck(exceptionIndex)

                // Load continuation argument just like suspending function returns it
                load(dataIndex, AsmTypes.OBJECT_TYPE)

                visitLabel(continuationLabelAfterLoadedResult.label)

                // Extend next instruction linenumber. Can't use line number of suspension point here because both non-suspended execution
                // and re-entering after suspension passes this label.
                val afterSuspensionPointLineNumber = nextLineNumberNode?.line ?: suspendElementLineNumber
                visitLineNumber(afterSuspensionPointLineNumber, continuationLabelAfterLoadedResult.label)
            })

            if (nextLineNumberNode != null) {
                // Remove the line number instruction as it now covered with line number on continuation label.
                // If both linenumber are present in bytecode, debugger will trigger line specific events twice.
                remove(nextLineNumberNode)
            }
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

private fun InstructionAdapter.generateResumeWithExceptionCheck(exceptionIndex: Int) {
    // Check if resumeWithException has been called
    load(exceptionIndex, AsmTypes.OBJECT_TYPE)
    dup()
    val noExceptionLabel = Label()
    ifnull(noExceptionLabel)
    athrow()

    mark(noExceptionLabel)
    pop()
}

private fun Type.fieldNameForVar(index: Int) = descriptor.first() + "$" + index

inline fun withInstructionAdapter(block: InstructionAdapter.() -> Unit): InsnList {
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
 * ICONST_0
 * INVOKESTATIC InlineMarker.mark()
 * INVOKEVIRTUAL suspensionMethod()Ljava/lang/Object; // actually it could be some inline method instead of plain call
 * CHECKCAST Type
 * ICONST_1
 * INVOKESTATIC InlineMarker.mark()
 */
private class SuspensionPoint(
        // ICONST_0
        val suspensionCallBegin: AbstractInsnNode,
        // INVOKESTATIC InlineMarker.mark()
        val suspensionCallEnd: AbstractInsnNode
) {
    lateinit var tryCatchBlocksContinuationLabel: LabelNode

    fun removeBeforeSuspendMarker(methodNode: MethodNode) {
        methodNode.instructions.remove(suspensionCallBegin.next)
        methodNode.instructions.remove(suspensionCallBegin)
    }

    fun removeAfterSuspendMarker(methodNode: MethodNode) {
        methodNode.instructions.remove(suspensionCallEnd.previous)
        methodNode.instructions.remove(suspensionCallEnd)
    }
}

private fun getLastParameterIndex(desc: String, access: Int) =
        Type.getArgumentTypes(desc).dropLast(1).map { it.size }.sum() + (if (!isStatic(access)) 1 else 0)

private fun getParameterTypesForCoroutineConstructor(desc: String, hasDispatchReceiver: Boolean, thisName: String) =
        listOfNotNull(if (!hasDispatchReceiver) null else Type.getObjectType(thisName)).toTypedArray() +
        Type.getArgumentTypes(desc).last()

private fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0

private fun getParameterTypesIndicesForCoroutineConstructor(
        desc: String,
        containingFunctionAccess: Int,
        needDispatchReceiver: Boolean,
        thisName: String
): Collection<Pair<Type, Int>> {
    return mutableListOf<Pair<Type, Int>>().apply {
        if (needDispatchReceiver) {
            add(Type.getObjectType(thisName) to 0)
        }
        val continuationIndex =
                getAllParameterTypes(desc, !isStatic(containingFunctionAccess), thisName).dropLast(1).map(Type::getSize).sum()
        add(CONTINUATION_ASM_TYPE to continuationIndex)
    }
}

private fun getAllParameterTypes(desc: String, hasDispatchReceiver: Boolean, thisName: String) =
        listOfNotNull(if (!hasDispatchReceiver) null else Type.getObjectType(thisName)).toTypedArray() +
        Type.getArgumentTypes(desc)

private fun allSuspensionPointsAreTailCalls(
        thisName: String,
        methodNode: MethodNode,
        suspensionPoints: List<SuspensionPoint>
): Boolean {
    val safelyReachableReturns = findSafelyReachableReturns(methodNode)
    val sourceFrames = MethodTransformer.analyze(thisName, methodNode, IgnoringCopyOperationSourceInterpreter())

    val instructions = methodNode.instructions
    return suspensionPoints.all { suspensionPoint ->
        val beginIndex = instructions.indexOf(suspensionPoint.suspensionCallBegin)
        val endIndex = instructions.indexOf(suspensionPoint.suspensionCallEnd)

        safelyReachableReturns[endIndex + 1]?.all { returnIndex ->
            val sourceInsn =
                    sourceFrames[returnIndex].top().sure {
                        "There must be some value on stack to return"
                    }.insns.singleOrNull()

            sourceInsn?.let(instructions::indexOf) in beginIndex..endIndex
        } ?: false
    }
}

private class IgnoringCopyOperationSourceInterpreter : SourceInterpreter() {
    override fun copyOperation(insn: AbstractInsnNode?, value: SourceValue?) = value
}

/**
 * Let's call an instruction safe if its execution is always invisible: stack modifications, branching, variable insns (invisible in debug)
 *
 * For some instruction `insn` define the result as following:
 * - if there is a path leading to the non-safe instruction then result is `null`
 * - Otherwise result contains all the reachable ARETURN indices
 *
 * @return indices of safely reachable returns for each instruction in the method node
 */
private fun findSafelyReachableReturns(methodNode: MethodNode): Array<Set<Int>?> {
    val controlFlowGraph = ControlFlowGraph.build(methodNode)

    val insns = methodNode.instructions
    val reachableReturnsIndices = Array<Set<Int>?>(insns.size()) init@{ index ->
        val insn = insns[index]

        if (insn.opcode == Opcodes.ARETURN) {
            return@init setOf(index)
        }

        if (!insn.isMeaningful || insn.opcode in SAFE_OPCODES || insn.isInvisibleInDebugVarInsn(methodNode) ||
            isInlineMarker(insn)) {
            setOf()
        }
        else null
    }

    var changed: Boolean
    do {
        changed = false
        for (index in 0 until insns.size()) {
            if (insns[index].opcode == Opcodes.ARETURN) continue

            @Suppress("RemoveExplicitTypeArguments")
            val newResult =
                    controlFlowGraph
                            .getSuccessorsIndices(index).plus(index)
                            .map(reachableReturnsIndices::get)
                            .fold<Set<Int>?, Set<Int>?>(mutableSetOf<Int>()) { acc, successorsResult ->
                                if (acc != null && successorsResult != null) acc + successorsResult else null
                            }

            if (newResult != reachableReturnsIndices[index]) {
                reachableReturnsIndices[index] = newResult
                changed = true
            }
        }
    } while (changed)

    return reachableReturnsIndices
}

private fun AbstractInsnNode?.isInvisibleInDebugVarInsn(methodNode: MethodNode): Boolean {
    val insns = methodNode.instructions
    val index = insns.indexOf(this)
    return (this is VarInsnNode && methodNode.localVariables.none {
        it.index == `var` && index in it.start.let(insns::indexOf)..it.end.let(insns::indexOf)
    })
}

private val SAFE_OPCODES =
        ((Opcodes.DUP..Opcodes.DUP2_X2) + Opcodes.NOP + Opcodes.POP + Opcodes.POP2 + (Opcodes.IFEQ..Opcodes.GOTO)).toSet()
