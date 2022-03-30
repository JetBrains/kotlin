/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.optimization.common.*
import org.jetbrains.kotlin.codegen.optimization.fixStack.FixStackMethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import kotlin.math.max

private const val COROUTINES_DEBUG_METADATA_VERSION = 1

private const val COROUTINES_METADATA_SOURCE_FILE_JVM_NAME = "f"
private const val COROUTINES_METADATA_LINE_NUMBERS_JVM_NAME = "l"
private const val COROUTINES_METADATA_LOCAL_NAMES_JVM_NAME = "n"
private const val COROUTINES_METADATA_SPILLED_JVM_NAME = "s"
private const val COROUTINES_METADATA_INDEX_TO_LABEL_JVM_NAME = "i"
private const val COROUTINES_METADATA_METHOD_NAME_JVM_NAME = "m"
private const val COROUTINES_METADATA_CLASS_NAME_JVM_NAME = "c"
private const val COROUTINES_METADATA_VERSION_JVM_NAME = "v"

const val SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME = "\$completion"
const val SUSPEND_CALL_RESULT_NAME = "\$result"
const val ILLEGAL_STATE_ERROR_MESSAGE = "call to 'resume' before 'invoke' with coroutine"

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
    // Since tail-call optimization of functions with Unit return type relies on ability of call-site to recognize them,
    // in order to ignore return value and push Unit, when we cannot ensure this ability, for example, when the function overrides function,
    // returning Any, we need to disable tail-call optimization for these functions.
    private val disableTailCallOptimizationForFunctionReturningUnit: Boolean,
    private val reportSuspensionPointInsideMonitor: (String) -> Unit,
    private val lineNumber: Int,
    private val sourceFile: String,
    // It's only matters for named functions, may differ from '!isStatic(access)' in case of DefaultImpls
    private val needDispatchReceiver: Boolean = false,
    // May differ from containingClassInternalName in case of DefaultImpls
    private val internalNameForDispatchReceiver: String? = null,
    // JVM_IR backend generates $completion, while old backend does not
    private val putContinuationParameterToLvt: Boolean = true,
    // Parameters of suspend lambda are put to the same fields as spilled variables
    private val initialVarsCountByType: Map<Type, Int> = emptyMap()
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {

    private val classBuilderForCoroutineState: ClassBuilder by lazy(obtainClassBuilderForCoroutineState)

    private var continuationIndex = if (isForNamedFunction) -1 else 0
    private var dataIndex = if (isForNamedFunction) -1 else 1

    override fun performTransformations(methodNode: MethodNode) {
        removeFakeContinuationConstructorCall(methodNode)

        replaceReturnsUnitMarkersWithPushingUnitOnStack(methodNode)

        replaceFakeContinuationsWithRealOnes(
            methodNode,
            if (isForNamedFunction) getLastParameterIndex(methodNode.desc, methodNode.access) else 0
        )

        FixStackMethodTransformer().transform(containingClassInternalName, methodNode)
        val suspensionPoints = collectSuspensionPoints(methodNode)
        RedundantLocalsEliminationMethodTransformer(suspensionPoints)
            .transform(containingClassInternalName, methodNode)
        ChangeBoxingMethodTransformer.transform(containingClassInternalName, methodNode)
        methodNode.updateMaxStack()

        checkForSuspensionPointInsideMonitor(methodNode, suspensionPoints)

        // First instruction in the method node may change in case of named function
        val actualCoroutineStart = methodNode.instructions.first

        if (isForNamedFunction) {
            if (putContinuationParameterToLvt) {
                addCompletionParameterToLVT(methodNode)
            }

            val examiner = MethodNodeExaminer(
                containingClassInternalName,
                methodNode,
                suspensionPoints,
                disableTailCallOptimizationForFunctionReturningUnit
            )
            if (examiner.allSuspensionPointsAreTailCalls(suspensionPoints)) {
                examiner.replacePopsBeforeSafeUnitInstancesWithCoroutineSuspendedChecks()
                examiner.addCoroutineSuspendedChecksBeforeSafeCheckcasts()
                dropSuspensionMarkers(methodNode)
                dropUnboxInlineClassMarkers(methodNode, suspensionPoints)
                return
            }

            dataIndex = methodNode.maxLocals++
            continuationIndex = methodNode.maxLocals++

            prepareMethodNodePreludeForNamedFunction(methodNode)
        }

        for (suspensionPoint in suspensionPoints) {
            splitTryCatchBlocksContainingSuspensionPoint(methodNode, suspensionPoint)
        }

        // Actual max stack might be increased during the previous phases
        methodNode.updateMaxStack()

        UninitializedStoresProcessor(methodNode).run()

        val spilledToVariableMapping = spillVariables(suspensionPoints, methodNode)

        val suspendMarkerVarIndex = methodNode.maxLocals++

        val suspensionPointLineNumbers = suspensionPoints.map { findSuspensionPointLineNumber(it) }

        // Create states in state-machine, to which state-machine can jump
        val stateLabels = suspensionPoints.withIndex().map {
            transformCallAndReturnStateLabel(
                it.index + 1, it.value, methodNode, suspendMarkerVarIndex, suspensionPointLineNumbers[it.index]
            )
        }

        methodNode.instructions.apply {
            val tableSwitchLabel = LabelNode()
            val firstStateLabel = LabelNode()
            val defaultLabel = LabelNode()

            // tableswitch(this.label)
            insertBefore(
                actualCoroutineStart,
                insnListOf(
                    *withInstructionAdapter { loadCoroutineSuspendedMarker() }.toArray(),
                    tableSwitchLabel,
                    // Allow debugger to stop on enter into suspend function
                    LineNumberNode(lineNumber, tableSwitchLabel),
                    VarInsnNode(Opcodes.ASTORE, suspendMarkerVarIndex),
                    VarInsnNode(Opcodes.ALOAD, continuationIndex),
                    *withInstructionAdapter { getLabel() }.toArray(),
                    TableSwitchInsnNode(
                        0,
                        suspensionPoints.size,
                        defaultLabel,
                        firstStateLabel, *stateLabels.toTypedArray()
                    ),
                    firstStateLabel
                )
            )

            insert(firstStateLabel, withInstructionAdapter {
                generateResumeWithExceptionCheck(dataIndex)
            })
            insert(last, defaultLabel)

            insert(last, withInstructionAdapter {
                AsmUtil.genThrow(this, "java/lang/IllegalStateException", ILLEGAL_STATE_ERROR_MESSAGE)
                areturn(Type.VOID_TYPE)
            })
        }

        initializeFakeInlinerVariables(methodNode, stateLabels)

        dropSuspensionMarkers(methodNode)
        dropUnboxInlineClassMarkers(methodNode, suspensionPoints)
        methodNode.removeEmptyCatchBlocks()

        updateLvtAccordingToLiveness(methodNode, isForNamedFunction, stateLabels)

        writeDebugMetadata(methodNode, suspensionPointLineNumbers, spilledToVariableMapping)
    }

    // When suspension point is inlined, it is in range of fake inliner variables.
    // Path from TABLESWITCH into unspilling goes to latter part of the range.
    // In this case the variables are uninitialized, initialize them, and split the local variable
    // range so that the local variable is only defined when initialized.
    private fun initializeFakeInlinerVariables(methodNode: MethodNode, stateLabels: List<LabelNode>) {
        for (stateLabel in stateLabels) {
            val newRecords = mutableListOf<LocalVariableNode>()
            for (record in methodNode.localVariables) {
                if (isFakeLocalVariableForInline(record.name) &&
                    methodNode.instructions.indexOf(record.start) < methodNode.instructions.indexOf(stateLabel) &&
                    methodNode.instructions.indexOf(stateLabel) < methodNode.instructions.indexOf(record.end)
                ) {
                    val newEnd = record.end
                    val newStart = LabelNode()
                    record.end = stateLabel
                    methodNode.instructions.insert(stateLabel, withInstructionAdapter {
                        iconst(0)
                        store(record.index, Type.INT_TYPE)
                    }.also {
                        it.add(newStart)
                    })
                    newRecords.add(
                        LocalVariableNode(
                            record.name,
                            record.desc,
                            record.signature,
                            newStart,
                            newEnd,
                            record.index
                        )
                    )
                }
            }
            methodNode.localVariables.addAll(newRecords)
        }
    }

    private fun addCompletionParameterToLVT(methodNode: MethodNode) {
        val index =
            /*  all args */ Type.getMethodType(methodNode.desc).argumentTypes.fold(0) { a, b -> a + b.size } +
                /* this */ (if (isStatic(methodNode.access)) 0 else 1) -
                /* only last */ 1
        val startLabel = with(methodNode.instructions) {
            if (first is LabelNode) first as LabelNode
            else LabelNode().also { insertBefore(first, it) }
        }

        val endLabel = with(methodNode.instructions) {
            if (last is LabelNode) last as LabelNode
            else LabelNode().also { insert(last, it) }
        }

        // When compiling with the old backend against the bytecode generated by JVM IR, $completion may already be in LVT.
        if (methodNode.localVariables.any {
                it.name == SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME &&
                        areLabelsBeforeSameInsn(startLabel, it.start) &&
                        areLabelsBeforeSameInsn(endLabel, it.end)
            }) {
            return
        }

        methodNode.localVariables.add(
            LocalVariableNode(
                SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME,
                CONTINUATION_ASM_TYPE.descriptor,
                null,
                startLabel,
                endLabel,
                index
            )
        )
    }

    /* Put { POP, GETSTATIC Unit } after suspension point if suspension point is a call of suspend function, that returns Unit.
     *
     * Otherwise, upon resume, the function would seem to not return Unit, despite being declared as returning Unit.
     *
     * This happens when said function is tail-call and its callee does not return Unit.
     *
     * Let's have an example
     *
     *   suspend fun int(): Int = suspendCoroutine { ...; 1 }
     *
     *   suspend fun unit() {
     *     int()
     *   }
     *
     *   suspend fun main() {
     *     println(unit())
     *   }
     *
     * So, in order to understand the necessity of { POP, GETSTATIC Unit } inside `main`, we need to consider two different scenarios
     *
     *   1. `unit` is not a tail-call function.
     *   2. `unit` is a tail-call function.
     *
     * When `unit` is a not tail-call function, calling `resumeWith` on its continuation will resume `unit`,
     * it will hit { GETSTATIC Unit; ARETURN } and this Unit will be the result of the suspend call. `unit`'s continuation will then call
     * `main` continuation's `resumeWith`, passing the Unit instance. The continuation in turn will resume `main` and the Unit will be
     * the result of `unit()` call. This result will then printed.
     *
     * However, when `unit` is a tail-call function, there is no continuation, generated for it. This is the point of tail-call
     * optimization. Thus, resume call will skip `unit` and land direcly in `main` continuation's `resumeWith`. And its result is not
     * Unit. Thus, we must ignore this result on call-site and use Unit instead. In other words, POP the result and GETSTATIC Unit
     * instead.
     */
    private fun replaceReturnsUnitMarkersWithPushingUnitOnStack(methodNode: MethodNode) {
        for (marker in methodNode.instructions.asSequence().filter(::isReturnsUnitMarker).toList()) {
            assert(marker.next?.next?.let { isAfterSuspendMarker(it) } == true) {
                "Expected AfterSuspendMarker after ReturnUnitMarker, got ${marker.next?.next}"
            }
            methodNode.instructions.insert(
                marker.next.next,
                withInstructionAdapter {
                    pop()
                    getstatic("kotlin/Unit", "INSTANCE", "Lkotlin/Unit;")
                }
            )
            methodNode.instructions.removeAll(listOf(marker.previous, marker))
        }
    }

    private fun findSuspensionPointLineNumber(suspensionPoint: SuspensionPoint) =
        suspensionPoint.suspensionCallBegin.findPreviousOrNull { it is LineNumberNode } as LineNumberNode?

    private fun checkForSuspensionPointInsideMonitor(methodNode: MethodNode, suspensionPoints: List<SuspensionPoint>) {
        if (methodNode.instructions.asSequence().none { it.opcode == Opcodes.MONITORENTER }) return

        val cfg = ControlFlowGraph.build(methodNode)
        val monitorDepthMap = hashMapOf<AbstractInsnNode, Int>()
        fun addMonitorDepthToSuccs(index: Int, depth: Int) {
            val insn = methodNode.instructions[index]
            monitorDepthMap[insn] = depth
            val newDepth = when (insn.opcode) {
                Opcodes.MONITORENTER -> depth + 1
                Opcodes.MONITOREXIT -> depth - 1
                else -> depth
            }
            for (succIndex in cfg.getSuccessorsIndices(index)) {
                if (monitorDepthMap[methodNode.instructions[succIndex]] == null) {
                    addMonitorDepthToSuccs(succIndex, newDepth)
                }
            }
        }

        addMonitorDepthToSuccs(0, 0)

        for (suspensionPoint in suspensionPoints) {
            if (monitorDepthMap[suspensionPoint.suspensionCallBegin]?.let { it > 0 } == true) {
                // TODO: Support crossinline suspend lambdas
                val stackTraceElement = StackTraceElement(
                    containingClassInternalName,
                    methodNode.name,
                    sourceFile,
                    findSuspensionPointLineNumber(suspensionPoint)?.line ?: -1
                )
                reportSuspensionPointInsideMonitor("$stackTraceElement")
                return
            }
        }
    }

    private fun writeDebugMetadata(
        methodNode: MethodNode,
        suspensionPointLineNumbers: List<LineNumberNode?>,
        spilledToLocalMapping: List<List<SpilledVariableAndField>>
    ) {
        val lines = suspensionPointLineNumbers.map { it?.line ?: -1 }
        val metadata = classBuilderForCoroutineState.newAnnotation(DEBUG_METADATA_ANNOTATION_ASM_TYPE.descriptor, true)
        metadata.visit(COROUTINES_METADATA_SOURCE_FILE_JVM_NAME, sourceFile)
        metadata.visit(COROUTINES_METADATA_LINE_NUMBERS_JVM_NAME, lines.toIntArray())

        val debugIndexToLabel = spilledToLocalMapping.withIndex().flatMap { (labelIndex, list) ->
            list.map { labelIndex }
        }
        val variablesMapping = spilledToLocalMapping.flatten()
        metadata.visit(COROUTINES_METADATA_INDEX_TO_LABEL_JVM_NAME, debugIndexToLabel.toIntArray())
        metadata.visitArray(COROUTINES_METADATA_SPILLED_JVM_NAME).also { v ->
            variablesMapping.forEach { v.visit(null, it.fieldName) }
        }.visitEnd()
        metadata.visitArray(COROUTINES_METADATA_LOCAL_NAMES_JVM_NAME).also { v ->
            variablesMapping.forEach { v.visit(null, it.variableName) }
        }.visitEnd()
        metadata.visit(COROUTINES_METADATA_METHOD_NAME_JVM_NAME, methodNode.name)
        metadata.visit(COROUTINES_METADATA_CLASS_NAME_JVM_NAME, Type.getObjectType(containingClassInternalName).className)
        @Suppress("ConstantConditionIf")
        if (COROUTINES_DEBUG_METADATA_VERSION != 1) {
            metadata.visit(COROUTINES_METADATA_VERSION_JVM_NAME, COROUTINES_DEBUG_METADATA_VERSION)
        }
        metadata.visitEnd()
    }

    // Warning! This is _continuation_, not _completion_, it can be allocated inside the method, thus, it is incorrect to treat it
    // as a parameter
    private fun addContinuationAndResultToLvt(
        methodNode: MethodNode,
        startLabel: Label,
        resultStartLabel: Label
    ) {
        val endLabel = Label()
        methodNode.instructions.add(withInstructionAdapter { mark(endLabel) })
        methodNode.visitLocalVariable(
            CONTINUATION_VARIABLE_NAME,
            CONTINUATION_ASM_TYPE.descriptor,
            null,
            startLabel,
            endLabel,
            continuationIndex
        )
        methodNode.visitLocalVariable(
            SUSPEND_CALL_RESULT_NAME,
            AsmTypes.OBJECT_TYPE.descriptor,
            null,
            resultStartLabel,
            endLabel,
            dataIndex
        )
    }

    private fun removeFakeContinuationConstructorCall(methodNode: MethodNode) {
        val seq = methodNode.instructions.asSequence()
        val first = seq.firstOrNull(::isBeforeFakeContinuationConstructorCallMarker)?.previous ?: return
        val last = seq.firstOrNull(::isAfterFakeContinuationConstructorCallMarker).sure {
            "BeforeFakeContinuationConstructorCallMarker without AfterFakeContinuationConstructorCallMarker"
        }
        val toRemove = InsnSequence(first, last).toList()
        methodNode.instructions.removeAll(toRemove)
        methodNode.instructions.set(last, InsnNode(Opcodes.ACONST_NULL))
    }

    private fun InstructionAdapter.getLabel() {
        getfield(
            Type.getObjectType(classBuilderForCoroutineState.thisName).internalName,
            COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor
        )
    }

    private fun InstructionAdapter.setLabel() {
        putfield(
            Type.getObjectType(classBuilderForCoroutineState.thisName).internalName,
            COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor
        )
    }

    private fun prepareMethodNodePreludeForNamedFunction(methodNode: MethodNode) {
        val objectTypeForState = Type.getObjectType(classBuilderForCoroutineState.thisName)
        val continuationArgumentIndex = getLastParameterIndex(methodNode.desc, methodNode.access)
        methodNode.instructions.asSequence().filterIsInstance<VarInsnNode>().forEach {
            if (it.`var` != continuationArgumentIndex) return@forEach
            assert(it.opcode == Opcodes.ALOAD) { "Only ALOADs are allowed for continuation arguments" }
            it.`var` = continuationIndex
        }

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

            visitVarInsn(Opcodes.ALOAD, continuationArgumentIndex)
            instanceOf(objectTypeForState)
            ifeq(createStateInstance)

            visitVarInsn(Opcodes.ALOAD, continuationArgumentIndex)
            checkcast(objectTypeForState)
            visitVarInsn(Opcodes.ASTORE, continuationIndex)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            getLabel()

            iconst(1 shl 31)
            and(Type.INT_TYPE)
            ifeq(createStateInstance)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            dup()
            getLabel()

            iconst(1 shl 31)
            sub(Type.INT_TYPE)
            setLabel()

            goTo(afterCoroutineStateCreated)

            visitLabel(createStateInstance)

            generateContinuationConstructorCall(
                objectTypeForState,
                methodNode,
                needDispatchReceiver,
                internalNameForDispatchReceiver,
                containingClassInternalName,
                classBuilderForCoroutineState
            )

            visitVarInsn(Opcodes.ASTORE, continuationIndex)

            visitLabel(afterCoroutineStateCreated)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            getfield(classBuilderForCoroutineState.thisName, CONTINUATION_RESULT_FIELD_NAME, AsmTypes.OBJECT_TYPE.descriptor)
            visitVarInsn(Opcodes.ASTORE, dataIndex)

            val resultStartLabel = Label()
            visitLabel(resultStartLabel)

            addContinuationAndResultToLvt(methodNode, afterCoroutineStateCreated, resultStartLabel)
        })
    }

    /*
     * Every suspension point should be surrounded by two markers: before suspension point marker (start marker)
     * and after suspension point marker (end marker)
     *
     * However, if suspension point comes from inline function and its end marker is unreachable, the end marker is removed by
     * either inliner or bytecode optimization.
     *
     * If this happens, we should restore end marker.
     *
     * Since in both cases (when end marker is reachable and when it is not) all paths should lead to
     * either a single end marker or to ATHROWs and ARETURNs, we just compute all paths from start marker until they reach
     * these instructions.
     */
    private fun collectSuspensionPoints(methodNode: MethodNode): List<SuspensionPoint> {
        // Exception paths lead outside suspension points, thus we should ignore them
        val cfg = ControlFlowGraph.build(methodNode, followExceptions = false)

        // DFS until end marker or ATHROW or ARETURN.
        // return true if it contains nested suspension points, which happens when we inline suspend lambda
        // with multiple suspension points via several inlines. See boxInline/state/stateMachine/passLambda.kt as an example.
        // In this case we simply ignore them.
        fun collectSuspensionPointEnds(
            insn: AbstractInsnNode,
            visited: MutableSet<AbstractInsnNode>,
            ends: MutableSet<AbstractInsnNode>
        ): Boolean {
            if (!visited.add(insn)) return false
            if (insn.opcode == Opcodes.ARETURN || insn.opcode == Opcodes.ATHROW || isAfterSuspendMarker(insn)) {
                ends.add(insn)
            } else {
                for (index in cfg.getSuccessorsIndices(insn)) {
                    val succ = methodNode.instructions[index]
                    if (isBeforeSuspendMarker(succ)) return true
                    if (collectSuspensionPointEnds(succ, visited, ends)) return true
                }
            }
            return false
        }

        return methodNode.instructions.asSequence().filter {
            isBeforeSuspendMarker(it)
        }.mapNotNull { start ->
            val ends = mutableSetOf<AbstractInsnNode>()
            if (collectSuspensionPointEnds(start, mutableSetOf(), ends)) return@mapNotNull null
            // Ignore suspension points, if the suspension call begin is alive and suspension call end is dead
            // (e.g., an inlined suspend function call ends with throwing a exception -- see KT-15017),
            // (also see boxInline/suspend/stateMachine/unreachableSuspendMarker.kt)
            // this is an exit point for the corresponding coroutine.
            val end = ends.find { isAfterSuspendMarker(it) } ?: return@mapNotNull null
            SuspensionPoint(start.previous, end)
        }.toList()
    }

    private fun dropSuspensionMarkers(methodNode: MethodNode) {
        // Drop markers, including ones, which we ignored in recognizing phase
        for (marker in methodNode.instructions.asSequence().filter { isBeforeSuspendMarker(it) || isAfterSuspendMarker(it) }.toList()) {
            methodNode.instructions.removeAll(listOf(marker.previous, marker))
        }
    }

    private fun dropUnboxInlineClassMarkers(methodNode: MethodNode, suspensionPoints: List<SuspensionPoint>) {
        for (marker in methodNode.instructions.asSequence().filter { isBeforeUnboxInlineClassMarker(it) }.toList()) {
            methodNode.instructions.removeAll(listOf(marker.previous, marker))
        }
        for (marker in methodNode.instructions.asSequence().filter { isAfterUnboxInlineClassMarker(it) }.toList()) {
            methodNode.instructions.removeAll(listOf(marker.previous.previous, marker.previous, marker))
        }
        for (suspension in suspensionPoints) {
            methodNode.instructions.removeAll(suspension.unboxInlineClassInstructions)
        }
    }

    private fun spillVariables(suspensionPoints: List<SuspensionPoint>, methodNode: MethodNode): List<List<SpilledVariableAndField>> {
        val instructions = methodNode.instructions
        val frames = performSpilledVariableFieldTypesAnalysis(methodNode, containingClassInternalName)

        fun AbstractInsnNode.index() = instructions.indexOf(this)

        val maxVarsCountByType = mutableMapOf<Type, Int>()
        var initialSpilledVariablesCount = 0
        for ((type, count) in initialVarsCountByType) {
            if (type == AsmTypes.OBJECT_TYPE) {
                initialSpilledVariablesCount = count
            }
            maxVarsCountByType[type] = count
        }

        val livenessFrames = analyzeLiveness(methodNode)

        // References shall be cleaned up after unspill (during spill in next suspension point) to prevent memory leaks,
        val referencesToSpillBySuspensionPointIndex = arrayListOf<List<ReferenceToSpill>>()
        // while primitives shall not
        val primitivesToSpillBySuspensionPointIndex = arrayListOf<List<PrimitiveToSpill>>()

        // Collect information about spillable variables, that we use to determine which variables we need to cleanup

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

            val referencesToSpill = arrayListOf<ReferenceToSpill>()
            val primitivesToSpill = arrayListOf<PrimitiveToSpill>()

            // 0 - this
            // 1 - parameter
            // ...
            // k - continuation
            // k + 1 - data
            // k + 2 - exception
            for (slot in 0 until localsCount) {
                if (slot == continuationIndex || slot == dataIndex) continue
                val value = frame.getLocal(slot)
                if (value.type == null || !livenessFrame.isAlive(slot)) continue

                if (value == StrictBasicValue.NULL_VALUE || value is TypedNullValue) {
                    referencesToSpill += slot to null
                    continue
                }

                val type = value.type!!
                val normalizedType = type.normalize()

                val indexBySort = varsCountByType[normalizedType]?.plus(1) ?: 0
                varsCountByType[normalizedType] = indexBySort

                val fieldName = normalizedType.fieldNameForVar(indexBySort)
                if (normalizedType == AsmTypes.OBJECT_TYPE) {
                    referencesToSpill += slot to SpillableVariable(value, type, normalizedType, fieldName)
                } else {
                    primitivesToSpill += slot to SpillableVariable(value, type, normalizedType, fieldName)
                }
            }

            referencesToSpillBySuspensionPointIndex += referencesToSpill
            primitivesToSpillBySuspensionPointIndex += primitivesToSpill

            for ((type, index) in varsCountByType) {
                maxVarsCountByType[type] = max(maxVarsCountByType[type] ?: 0, index)
            }
        }

        // Calculate variables to cleanup

        // Use CFG to calculate amount of spilled variables in previous suspension point (P) and current one (C).
        // All fields from L$C to L$P should be cleaned. I.e. we should spill ACONST_NULL to them.
        val cfg = ControlFlowGraph.build(methodNode)

        // Collect all immediately preceding suspension points. I.e. suspension points, from which there is a path
        // into current one, that does not cross other suspension points.
        val suspensionPointEnds = suspensionPoints.associateBy { it.suspensionCallEnd }
        fun findSuspensionPointPredecessors(suspension: SuspensionPoint): List<SuspensionPoint> {
            val visited = mutableSetOf<AbstractInsnNode>()
            fun dfs(current: AbstractInsnNode): List<SuspensionPoint> {
                if (!visited.add(current)) return emptyList()
                suspensionPointEnds[current]?.let { return listOf(it) }
                return cfg.getPredecessorsIndices(current).flatMap { dfs(instructions[it]) }
            }
            return dfs(suspension.suspensionCallBegin)
        }

        val predSuspensionPoints = suspensionPoints.associateWith { findSuspensionPointPredecessors(it) }

        // Calculate all pairs SuspensionPoint -> C and P, where P is minimum of all preds' Cs
        fun countVariablesToSpill(index: Int): Int =
            referencesToSpillBySuspensionPointIndex[index].count { (_, variable) -> variable != null }

        val referencesToCleanBySuspensionPointIndex = arrayListOf<Pair<Int, Int>>() // current to pred
        for (suspensionPointIndex in suspensionPoints.indices) {
            val suspensionPoint = suspensionPoints[suspensionPointIndex]
            val currentSpilledReferencesCount = countVariablesToSpill(suspensionPointIndex)
            val preds = predSuspensionPoints[suspensionPoint]
            val predSpilledReferencesCount =
                if (preds.isNullOrEmpty()) initialSpilledVariablesCount
                else preds.maxOf { countVariablesToSpill(suspensionPoints.indexOf(it)) }
            referencesToCleanBySuspensionPointIndex += currentSpilledReferencesCount to predSpilledReferencesCount
        }

        // Calculate debug metadata mapping before modifying method node to make it easier to locate
        // locals alive across suspension points.

        fun calculateSpilledVariableAndField(
            suspension: SuspensionPoint,
            slot: Int,
            spillableVariable: SpillableVariable?
        ): SpilledVariableAndField? {
            if (spillableVariable == null) return null
            val name = localVariableName(methodNode, slot, suspension.suspensionCallBegin.index()) ?: return null
            return SpilledVariableAndField(spillableVariable.fieldName, name)
        }

        val spilledToVariableMapping = arrayListOf<List<SpilledVariableAndField>>()
        for (suspensionPointIndex in suspensionPoints.indices) {
            val suspension = suspensionPoints[suspensionPointIndex]

            val spilledToVariable = arrayListOf<SpilledVariableAndField>()

            referencesToSpillBySuspensionPointIndex[suspensionPointIndex].mapNotNullTo(spilledToVariable) { (slot, spillableVariable) ->
                calculateSpilledVariableAndField(suspension, slot, spillableVariable)
            }
            primitivesToSpillBySuspensionPointIndex[suspensionPointIndex].mapNotNullTo(spilledToVariable) { (slot, spillableVariable) ->
                calculateSpilledVariableAndField(suspension, slot, spillableVariable)
            }

            spilledToVariableMapping += spilledToVariable
        }

        // Mutate method node

        fun generateSpillAndUnspill(suspension: SuspensionPoint, slot: Int, spillableVariable: SpillableVariable?) {
            fun splitLvtRecord(local: LocalVariableNode?, localRestart: LabelNode) {
                // Split the local variable range for the local so that it is visible until the next state label, but is
                // not visible until it has been unspilled from the continuation on the reentry path.
                if (local != null) {
                    val previousEnd = local.end
                    local.end = suspension.stateLabel
                    // Add the local back, but end it at the next state label.
                    methodNode.localVariables.add(local)
                    // Add a new entry that starts after the local variable is restored from the continuation.
                    methodNode.localVariables.add(
                        LocalVariableNode(
                            local.name,
                            local.desc,
                            local.signature,
                            localRestart,
                            previousEnd,
                            local.index
                        )
                    )
                }
            }

            // Find and remove the local variable node, if any, in the local variable table corresponding to the slot that is spilled.
            var local: LocalVariableNode? = null
            val iterator = methodNode.localVariables.listIterator()
            while (iterator.hasNext()) {
                val node = iterator.next()
                if (node.index == slot &&
                    methodNode.instructions.indexOf(node.start) <= methodNode.instructions.indexOf(suspension.suspensionCallBegin) &&
                    methodNode.instructions.indexOf(node.end) > methodNode.instructions.indexOf(suspension.tryCatchBlockEndLabelAfterSuspensionCall)
                ) {
                    local = node
                    iterator.remove()
                    break
                }
            }

            if (spillableVariable == null) {
                with(instructions) {
                    insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                        aconst(null)
                        store(slot, AsmTypes.OBJECT_TYPE)
                    })
                }
                val newStart = suspension.tryCatchBlocksContinuationLabel.findNextOrNull { it is LabelNode } as? LabelNode ?: return
                splitLvtRecord(local, newStart)
                return
            }

            val localRestart = LabelNode().linkWithLabel()
            with(instructions) {
                // store variable before suspension call
                insertBefore(suspension.suspensionCallBegin, withInstructionAdapter {
                    load(continuationIndex, AsmTypes.OBJECT_TYPE)
                    load(slot, spillableVariable.type)
                    StackValue.coerce(spillableVariable.type, spillableVariable.normalizedType, this)
                    putfield(
                        classBuilderForCoroutineState.thisName,
                        spillableVariable.fieldName,
                        spillableVariable.normalizedType.descriptor
                    )
                })

                // restore variable after suspension call
                insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                    load(continuationIndex, AsmTypes.OBJECT_TYPE)
                    getfield(
                        classBuilderForCoroutineState.thisName,
                        spillableVariable.fieldName,
                        spillableVariable.normalizedType.descriptor
                    )
                    StackValue.coerce(spillableVariable.normalizedType, spillableVariable.type, this)
                    store(slot, spillableVariable.type)
                    if (local != null) {
                        visitLabel(localRestart.label)
                    }
                })
            }

            splitLvtRecord(local, localRestart)
        }

        fun cleanUpField(suspension: SuspensionPoint, fieldIndex: Int) {
            with(instructions) {
                insertBefore(suspension.suspensionCallBegin, withInstructionAdapter {
                    load(continuationIndex, AsmTypes.OBJECT_TYPE)
                    aconst(null)
                    putfield(
                        classBuilderForCoroutineState.thisName,
                        "L\$$fieldIndex",
                        AsmTypes.OBJECT_TYPE.descriptor
                    )
                })
            }
        }

        for (suspensionPointIndex in suspensionPoints.indices) {
            val suspension = suspensionPoints[suspensionPointIndex]
            for ((slot, referenceToSpill) in referencesToSpillBySuspensionPointIndex[suspensionPointIndex]) {
                generateSpillAndUnspill(suspension, slot, referenceToSpill)
            }
            val (currentSpilledCount, predSpilledCount) = referencesToCleanBySuspensionPointIndex[suspensionPointIndex]
            if (predSpilledCount > currentSpilledCount) {
                for (fieldIndex in currentSpilledCount until predSpilledCount) {
                    cleanUpField(suspension, fieldIndex)
                }
            }
            for ((slot, primitiveToSpill) in primitivesToSpillBySuspensionPointIndex[suspensionPointIndex]) {
                generateSpillAndUnspill(suspension, slot, primitiveToSpill)
            }
        }

        for (entry in maxVarsCountByType) {
            val (type, maxIndex) = entry
            for (index in (initialVarsCountByType[type]?.plus(1) ?: 0)..maxIndex) {
                classBuilderForCoroutineState.newField(
                    JvmDeclarationOrigin.NO_ORIGIN, AsmUtil.NO_FLAG_PACKAGE_PRIVATE,
                    type.fieldNameForVar(index), type.descriptor, null, null
                )
            }
        }

        return spilledToVariableMapping
    }

    private fun localVariableName(
        methodNode: MethodNode,
        index: Int,
        suspensionCallIndex: Int
    ): String? {
        val variable = methodNode.localVariables.find {
            index == it.index && methodNode.instructions.indexOf(it.start) <= suspensionCallIndex
                    && suspensionCallIndex < methodNode.instructions.indexOf(it.end)
        }
        return variable?.name
    }

    /**
     * See 'splitTryCatchBlocksContainingSuspensionPoint'
     */
    private val SuspensionPoint.tryCatchBlockEndLabelAfterSuspensionCall: LabelNode
        get() {
            assert(suspensionCallEnd.next is LabelNode) {
                "Next instruction after $this should be a label, but " +
                        "${suspensionCallEnd.next::class.java}/${suspensionCallEnd.next.opcode} was found"
            }

            return suspensionCallEnd.next as LabelNode
        }

    private fun transformCallAndReturnStateLabel(
        id: Int,
        suspension: SuspensionPoint,
        methodNode: MethodNode,
        suspendMarkerVarIndex: Int,
        suspendPointLineNumber: LineNumberNode?
    ): LabelNode {
        val continuationLabelAfterLoadedResult = LabelNode()
        val suspendElementLineNumber = lineNumber
        var nextLineNumberNode = nextDefinitelyHitLineNumber(suspension)
        with(methodNode.instructions) {
            // Save state
            insertBefore(
                suspension.suspensionCallBegin,
                withInstructionAdapter {
                    visitVarInsn(Opcodes.ALOAD, continuationIndex)
                    iconst(id)
                    setLabel()
                }
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
                visitLabel(suspension.stateLabel.label)
            })

            // After suspension point there is always three nodes: L1, NOP, L2
            // And if there are relevant exception handlers, they always start at L2
            // See 'splitTryCatchBlocksContainingSuspensionPoint'
            val possibleTryCatchBlockStart = suspension.tryCatchBlocksContinuationLabel

            // Move NOP, which is inserted in `splitTryCatchBlocksContainingSuspentionPoint`, inside the try catch block,
            // so the inliner can transform suspend lambdas during inlining
            assert(possibleTryCatchBlockStart.previous.opcode == Opcodes.NOP) {
                "NOP expected but ${possibleTryCatchBlockStart.previous.opcode} was found"
            }
            remove(possibleTryCatchBlockStart.previous)

            insert(possibleTryCatchBlockStart, withInstructionAdapter {
                nop()
                generateResumeWithExceptionCheck(dataIndex)

                // Load continuation argument just like suspending function returns it
                load(dataIndex, AsmTypes.OBJECT_TYPE)
                // Unbox inline class, since this is the resume path and unlike the direct path
                // the class is boxed.
                for (insn in suspension.unboxInlineClassInstructions) {
                    insn.accept(this)
                }

                visitLabel(continuationLabelAfterLoadedResult.label)

                if (nextLineNumberNode != null) {
                    // If there is a clear next linenumber instruction, extend it. Can't use line number of suspension point
                    // here because both non-suspended execution and re-entering after suspension passes this label.
                    if (possibleTryCatchBlockStart.next?.opcode?.let {
                            it != Opcodes.ASTORE && it != Opcodes.CHECKCAST && it != Opcodes.INVOKESTATIC &&
                                    it != Opcodes.INVOKEVIRTUAL && it != Opcodes.INVOKEINTERFACE
                        } == true
                    ) {
                        visitLineNumber(nextLineNumberNode!!.line, continuationLabelAfterLoadedResult.label)
                    } else {
                        // But keep the linenumber if the result of the call is used afterwards
                        nextLineNumberNode = null
                    }
                } else if (suspendPointLineNumber != null) {
                    // If there is no clear next linenumber instruction, the continuation is still on the
                    // same line as the suspend point.
                    visitLineNumber(suspendPointLineNumber.line, continuationLabelAfterLoadedResult.label)
                }
            })

            if (nextLineNumberNode != null) {
                // Remove the line number instruction as it now covered with line number on continuation label.
                // If both linenumber are present in bytecode, debugger will trigger line specific events twice.
                remove(nextLineNumberNode)
            }
        }

        return suspension.stateLabel
    }

    // Find the next line number instruction that is defintely hit. That is, a line number
    // that comes before any branch or method call.
    private fun nextDefinitelyHitLineNumber(suspension: SuspensionPoint): LineNumberNode? {
        var next = suspension.suspensionCallEnd.next
        while (next != null) {
            when {
                next.isBranchOrCall -> return null
                next is LineNumberNode -> return next
                else -> next = next.next
            }
        }
        return null
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
                        "Try catch block ${instructions.indexOf(it.start)}:${instructions.indexOf(it.end)} containing marker before " +
                                "suspension point $beginIndex should also contain the marker after suspension point $endIndex"
                    }
                    listOf(
                        TryCatchBlockNode(it.start, firstLabel, it.handler, it.type),
                        TryCatchBlockNode(secondLabel, it.end, it.handler, it.type)
                    )
                } else
                    listOf(it)
            }

        suspensionPoint.tryCatchBlocksContinuationLabel = secondLabel

        return
    }

    private data class SpilledVariableAndField(val fieldName: String, val variableName: String)
}

private class SpillableVariable(
    val value: BasicValue,
    val type: Type,
    val normalizedType: Type,
    val fieldName: String
)

private typealias ReferenceToSpill = Pair<Int, SpillableVariable?>
private typealias PrimitiveToSpill = Pair<Int, SpillableVariable>

internal fun InstructionAdapter.generateContinuationConstructorCall(
    objectTypeForState: Type?,
    methodNode: MethodNode,
    needDispatchReceiver: Boolean,
    internalNameForDispatchReceiver: String?,
    containingClassInternalName: String,
    classBuilderForCoroutineState: ClassBuilder
) {
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
}

private fun InstructionAdapter.generateResumeWithExceptionCheck(dataIndex: Int) {
    // Check if resumeWithException has been called

    load(dataIndex, AsmTypes.OBJECT_TYPE)
    invokestatic("kotlin/ResultKt", "throwOnFailure", "(Ljava/lang/Object;)V", false)
}

private fun Type.fieldNameForVar(index: Int) = descriptor.first() + "$" + index

inline fun withInstructionAdapter(block: InstructionAdapter.() -> Unit): InsnList {
    val tmpMethodNode = MethodNode()

    InstructionAdapter(tmpMethodNode).apply(block)

    return tmpMethodNode.instructions
}

fun Type.normalize(): Type =
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
internal class SuspensionPoint(
    // ICONST_0
    val suspensionCallBegin: AbstractInsnNode,
    // INVOKESTATIC InlineMarker.mark()
    val suspensionCallEnd: AbstractInsnNode
) {
    lateinit var tryCatchBlocksContinuationLabel: LabelNode

    val stateLabel = LabelNode().linkWithLabel()
    val unboxInlineClassInstructions: List<AbstractInsnNode> = findUnboxInlineClassInstructions()

    private fun findUnboxInlineClassInstructions(): List<AbstractInsnNode> {
        val beforeMarker = suspensionCallEnd.next?.next ?: return emptyList()
        if (!isBeforeUnboxInlineClassMarker(beforeMarker)) return emptyList()
        val afterMarker = beforeMarker.findNextOrNull { isAfterUnboxInlineClassMarker(it) }
            ?: error("Before unbox inline class marker without after unbox inline class marker")
        return InsnSequence(beforeMarker.next, afterMarker.previous.previous).toList()
    }

    operator fun contains(insn: AbstractInsnNode): Boolean {
        for (i in InsnSequence(suspensionCallBegin, suspensionCallEnd.next)) {
            if (i == insn) return true
        }
        return false
    }
}

internal operator fun List<SuspensionPoint>.contains(insn: AbstractInsnNode): Boolean =
    any { insn in it }

internal fun getLastParameterIndex(desc: String, access: Int) =
    Type.getArgumentTypes(desc).dropLast(1).sumOf { it.size } + (if (!isStatic(access)) 1 else 0)

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
            getAllParameterTypes(desc, !isStatic(containingFunctionAccess), thisName).dropLast(1).sumOf(Type::getSize)
        add(CONTINUATION_ASM_TYPE to continuationIndex)
    }
}

private fun getAllParameterTypes(desc: String, hasDispatchReceiver: Boolean, thisName: String) =
    listOfNotNull(if (!hasDispatchReceiver) null else Type.getObjectType(thisName)).toTypedArray() +
            Type.getArgumentTypes(desc)

internal fun replaceFakeContinuationsWithRealOnes(methodNode: MethodNode, continuationIndex: Int) {
    val fakeContinuations = methodNode.instructions.asSequence().filter(::isFakeContinuationMarker).toList()
    for (fakeContinuation in fakeContinuations) {
        methodNode.instructions.removeAll(listOf(fakeContinuation.previous.previous, fakeContinuation.previous))
        methodNode.instructions.set(fakeContinuation, VarInsnNode(Opcodes.ALOAD, continuationIndex))
    }
}

@Suppress("unused")
private fun MethodNode.nodeTextWithLiveness(liveness: List<VariableLivenessFrame>): String =
    liveness.zip(this.instructions.asSequence().toList()).joinToString("\n") { (a, b) -> "$a|${b.insnText}" }

/* We do not want to spill dead variables, thus, we shrink its LVT record to region, where the variable is alive,
 * so, the variable will not be visible in debugger. User can still prolong life span of the variable by using it.
 *
 * This means, that function parameters do not longer span the whole function, including `this`.
 * This might and will break some bytecode processors, including old versions of R8. See KT-24510.
 */
private fun updateLvtAccordingToLiveness(method: MethodNode, isForNamedFunction: Boolean, suspensionPoints: List<LabelNode>) {
    val liveness = analyzeLiveness(method)

    fun List<LocalVariableNode>.findRecord(insnIndex: Int, variableIndex: Int): LocalVariableNode? {
        for (variable in this) {
            if (variable.index == variableIndex &&
                method.instructions.indexOf(variable.start) <= insnIndex &&
                insnIndex < method.instructions.indexOf(variable.end)
            ) return variable
        }
        return null
    }

    fun isAlive(insnIndex: Int, variableIndex: Int): Boolean =
        liveness[insnIndex].isAlive(variableIndex)

    fun nextLabel(node: AbstractInsnNode?): LabelNode? {
        var current = node
        while (current != null) {
            if (current is LabelNode) return current
            current = current.next
        }
        return null
    }

    fun min(a: LabelNode, b: LabelNode): LabelNode =
        if (method.instructions.indexOf(a) < method.instructions.indexOf(b)) a else b

    val oldLvt = arrayListOf<LocalVariableNode>()
    for (record in method.localVariables) {
        oldLvt += record
    }
    method.localVariables.clear()

    val oldLvtNodeToLatestNewLvtNode = mutableMapOf<LocalVariableNode, LocalVariableNode>()
    // Skip `this` for suspend lambda
    val start = if (isForNamedFunction) 0 else 1
    for (variableIndex in start until method.maxLocals) {
        if (oldLvt.none { it.index == variableIndex }) continue
        var startLabel: LabelNode? = null
        for (insnIndex in 0 until (method.instructions.size() - 1)) {
            val insn = method.instructions[insnIndex]
            if (!isAlive(insnIndex, variableIndex) && isAlive(insnIndex + 1, variableIndex)) {
                startLabel = insn as? LabelNode ?: insn.findNextOrNull { it is LabelNode } as? LabelNode
            }
            if (isAlive(insnIndex, variableIndex) && !isAlive(insnIndex + 1, variableIndex)) {
                // No variable in LVT -> do not add one
                val lvtRecord = oldLvt.findRecord(insnIndex, variableIndex) ?: continue
                if (lvtRecord.name == CONTINUATION_VARIABLE_NAME || lvtRecord.name == SUSPEND_CALL_RESULT_NAME) continue
                // End the local when it is no longer live and then attempt to extend its range when safe.
                val endLabel = nextLabel(insn.next)?.let { min(lvtRecord.end, it) } ?: lvtRecord.end
                // startLabel can be null in case of parameters
                @Suppress("NAME_SHADOWING") val startLabel = startLabel ?: lvtRecord.start

                // Attempt to extend existing local variable node corresponding to the record in
                // the original local variable table if there are no control-flow merges.
                val latest = oldLvtNodeToLatestNewLvtNode[lvtRecord]
                // If we can extend the previous range to where the local variable dies, we do not need a
                // new entry, we know we cannot extend it to the lvt.endOffset, if we could we would have
                // done so when we added it below.
                val extended = latest?.extendRecordIfPossible(method, suspensionPoints, lvtRecord.end, liveness) ?: false
                if (!extended) {
                    val new = LocalVariableNode(lvtRecord.name, lvtRecord.desc, lvtRecord.signature, startLabel, endLabel, lvtRecord.index)
                    oldLvtNodeToLatestNewLvtNode[lvtRecord] = new
                    method.localVariables.add(new)
                    // See if we can extend it all the way to the old end.
                    new.extendRecordIfPossible(method, suspensionPoints, lvtRecord.end, liveness)
                }
            }
        }
    }

    for (variable in oldLvt) {
        // $continuation and $result are dead, but they are used by debugger, as well as fake inliner variables
        // For example, $continuation is used to create async stack trace
        if (variable.name == CONTINUATION_VARIABLE_NAME ||
            variable.name == SUSPEND_CALL_RESULT_NAME ||
            isFakeLocalVariableForInline(variable.name)
        ) {
            method.localVariables.add(variable)
            continue
        }
        // this acts like $continuation for lambdas. For example, it is used by debugger to create async stack trace. Keep it.
        if (variable.name == "this" && !isForNamedFunction) {
            method.localVariables.add(variable)
            continue
        }
    }
}

/* We cannot extend a record if there is STORE instruction or a control-flow merge.
 *
 * STORE instructions can signify a unspilling operation, in which case, the variable will become visible before it unspilled.
 *
 * If there is a control-flow merge point in a range where a variable is dead, it might not have been restored on one of the paths
 * and therefore it is not safe to extend the record across the control flow merge point.
 *
 * For example, code such as the following:
 *
 *    listOf<String>.forEach {
 *       yield(it)
 *    }
 *
 * Generates code of this form with a back edge after resumption that will lead to invalid locals tables
 * if the local range is extended to the next suspension point. L1 is a merge point and therefore, we do
 * not extend.
 *
 *        iterator = iterable.iterator()
 *    L1: (iterable dies here)
 *        load iterator.next if there
 *        yield suspension point
 *
 *    L2: (resumption point)
 *        restore live variables (not including iterable)
 *        goto L1 (iterator not restored here, so we cannot not have iterator live at L1)
 *
 * Code such as:
 *
 *    val value = getValue()
 *    return if (value == null) {
 *        computeValueAsync()  // suspension point
 *    } else {
 *        value
 *    } + "K"
 *
 * Generates code of this form, where it is not safe to extend the `value` local variable across the control-flow
 * merge because it is dead and will not have been restored after the suspend point in one of the branches.
 *
 *      value = getValue()
 *      if (value != null) goto L2
 *  L1: (value dead here)
 *      temp = computeValueAsync() // suspension point and resumption point, value NOT restored as it is dead
 *      load temp
 *      goto L3
 *  L2: (value alive here)
 *      load value
 *  L3: (merge point, cannot extend `value` local across as it is not defined on one of the paths)
 *      load "K"
 *      add strings
 *      return
 *
 * @return true if the range has been extended
 */
private fun LocalVariableNode.extendRecordIfPossible(
    method: MethodNode,
    suspensionPoints: List<LabelNode>,
    endLabel: LabelNode,
    liveness: List<VariableLivenessFrame>
): Boolean {
    val nextSuspensionPointLabel = suspensionPoints.find { it in InsnSequence(end, endLabel) } ?: endLabel

    var current: AbstractInsnNode? = end
    var index = method.instructions.indexOf(current)
    while (current != null && current != nextSuspensionPointLabel) {
        if (liveness[index].isControlFlowMerge()) return false
        // TODO: HACK
        // TODO: Find correct label, which is OK to be used as end label.
        if (current.opcode == Opcodes.ARETURN && nextSuspensionPointLabel != endLabel) return false
        if (current.isStoreOperation() && (current as VarInsnNode).`var` == index) {
            return false
        }
        current = current.next
        ++index
    }
    end = nextSuspensionPointLabel
    return true
}
