/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.TransformationMethodVisitor
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.*
import org.jetbrains.kotlin.codegen.optimization.fixStack.FixStackMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.*
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
    private val shouldPreserveClassInitialization: Boolean,
    private val languageVersionSettings: LanguageVersionSettings,
    // These two are needed to report diagnostics about suspension points inside critical section
    private val element: KtElement,
    private val diagnostics: DiagnosticSink,
    // Since tail-call optimization of functions with Unit return type relies on ability of call-site to recognize them,
    // in order to ignore return value and push Unit, when we cannot ensure this ability, for example, when the function overrides function,
    // returning Any, we need to disable tail-call optimization for these functions.
    private val disableTailCallOptimizationForFunctionReturningUnit: Boolean,
    // It's only matters for named functions, may differ from '!isStatic(access)' in case of DefaultImpls
    private val needDispatchReceiver: Boolean = false,
    // May differ from containingClassInternalName in case of DefaultImpls
    private val internalNameForDispatchReceiver: String? = null,
    // JVM_IR backend generates $completion, while old backend does not
    private val putContinuationParameterToLvt: Boolean = true
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {

    private val classBuilderForCoroutineState: ClassBuilder by lazy(obtainClassBuilderForCoroutineState)
    private val lineNumber = CodegenUtil.getLineNumberForElement(element, false) ?: 0
    private val sourceFile = element.containingKtFile.name

    private var continuationIndex = if (isForNamedFunction) -1 else 0
    private var dataIndex = if (isForNamedFunction) -1 else 1
    private var exceptionIndex = if (isForNamedFunction || languageVersionSettings.isReleaseCoroutines()) -1 else 2

    override fun performTransformations(methodNode: MethodNode) {
        removeFakeContinuationConstructorCall(methodNode)

        replaceReturnsUnitMarkersWithPushingUnitOnStack(methodNode)

        replaceFakeContinuationsWithRealOnes(
            methodNode,
            if (isForNamedFunction) getLastParameterIndex(methodNode.desc, methodNode.access) else 0
        )

        FixStackMethodTransformer().transform(containingClassInternalName, methodNode)
        RedundantLocalsEliminationMethodTransformer(languageVersionSettings).transform(containingClassInternalName, methodNode)
        if (languageVersionSettings.isReleaseCoroutines()) {
            ChangeBoxingMethodTransformer.transform(containingClassInternalName, methodNode)
        }
        updateMaxStack(methodNode)

        val suspensionPoints = collectSuspensionPoints(methodNode)

        checkForSuspensionPointInsideMonitor(methodNode, suspensionPoints)

        // First instruction in the method node may change in case of named function
        val actualCoroutineStart = methodNode.instructions.first

        if (isForNamedFunction) {
            if (putContinuationParameterToLvt) {
                addCompletionParameterToLVT(methodNode)
            }

            val examiner = MethodNodeExaminer(
                languageVersionSettings,
                containingClassInternalName,
                methodNode,
                disableTailCallOptimizationForFunctionReturningUnit
            )
            if (examiner.allSuspensionPointsAreTailCalls(suspensionPoints)) {
                examiner.replacePopsBeforeSafeUnitInstancesWithCoroutineSuspendedChecks()
                dropSuspensionMarkers(methodNode)
                return
            }

            dataIndex = methodNode.maxLocals++
            if (!languageVersionSettings.isReleaseCoroutines()) {
                exceptionIndex = methodNode.maxLocals++
            }
            continuationIndex = methodNode.maxLocals++

            prepareMethodNodePreludeForNamedFunction(methodNode)
        }

        for (suspensionPoint in suspensionPoints) {
            splitTryCatchBlocksContainingSuspensionPoint(methodNode, suspensionPoint)
        }

        // Actual max stack might be increased during the previous phases
        updateMaxStack(methodNode)

        UninitializedStoresProcessor(methodNode, shouldPreserveClassInitialization).run()

        val spilledToVariableMapping = spillVariables(suspensionPoints, methodNode)

        val suspendMarkerVarIndex = methodNode.maxLocals++

        val suspensionPointLineNumbers = suspensionPoints.map { findSuspensionPointLineNumber(it) }

        val continuationLabels = suspensionPoints.withIndex().map {
            transformCallAndReturnContinuationLabel(
                it.index + 1, it.value, methodNode, suspendMarkerVarIndex, suspensionPointLineNumbers[it.index])
        }

        methodNode.instructions.apply {
            val tableSwitchLabel = LabelNode()
            val firstStateLabel = LabelNode()
            val defaultLabel = LabelNode()

            // tableswitch(this.label)
            insertBefore(
                actualCoroutineStart,
                insnListOf(
                    *withInstructionAdapter { loadCoroutineSuspendedMarker(languageVersionSettings) }.toArray(),
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
                        firstStateLabel, *continuationLabels.toTypedArray()
                    ),
                    firstStateLabel
                )
            )

            insert(firstStateLabel, withInstructionAdapter {
                generateResumeWithExceptionCheck(languageVersionSettings.isReleaseCoroutines(), dataIndex, exceptionIndex)
            })
            insert(last, defaultLabel)

            insert(last, withInstructionAdapter {
                AsmUtil.genThrow(this, "java/lang/IllegalStateException", ILLEGAL_STATE_ERROR_MESSAGE)
                areturn(Type.VOID_TYPE)
            })
        }

        dropSuspensionMarkers(methodNode)
        methodNode.removeEmptyCatchBlocks()

        // The parameters (and 'this') shall live throughout the method, otherwise, d8 emits warning about invalid debug info
        val startLabel = LabelNode()
        val endLabel = LabelNode()
        methodNode.instructions.insertBefore(methodNode.instructions.first, startLabel)
        methodNode.instructions.insert(methodNode.instructions.last, endLabel)

        fixLvtForParameters(methodNode, startLabel, endLabel)

        if (languageVersionSettings.isReleaseCoroutines()) {
            writeDebugMetadata(methodNode, suspensionPointLineNumbers, spilledToVariableMapping)
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
        methodNode.localVariables.add(
            LocalVariableNode(
                SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME,
                languageVersionSettings.continuationAsmType().descriptor,
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
                diagnostics.report(ErrorsJvm.SUSPENSION_POINT_INSIDE_MONITOR.on(element, "$stackTraceElement"))
                return
            }
        }
    }

    private fun fixLvtForParameters(methodNode: MethodNode, startLabel: LabelNode, endLabel: LabelNode) {
        val paramsNum =
                /* this */ (if (isStatic(methodNode.access)) 0 else 1) +
                /* real params */ Type.getArgumentTypes(methodNode.desc).fold(0) { a, b -> a + b.size }

        for (i in 0 until paramsNum) {
            fixRangeOfLvtRecord(methodNode, i, startLabel, endLabel)
        }
    }

    private fun fixRangeOfLvtRecord(methodNode: MethodNode, index: Int, startLabel: LabelNode, endLabel: LabelNode) {
        val vars = methodNode.localVariables.filter { it.index == index }
        assert(vars.size <= 1) {
            "Someone else occupies parameter's slot at $index"
        }
        vars.firstOrNull()?.let {
            it.start = startLabel
            it.end = endLabel
        }
    }

    private fun writeDebugMetadata(
        methodNode: MethodNode,
        suspensionPointLineNumbers: List<LineNumberNode?>,
        spilledToLocalMapping: List<List<SpilledVariableDescriptor>>
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
            languageVersionSettings.continuationAsmType().descriptor,
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
        if (isForNamedFunction && !languageVersionSettings.isReleaseCoroutines())
            invokevirtual(
                classBuilderForCoroutineState.thisName,
                "getLabel",
                Type.getMethodDescriptor(Type.INT_TYPE),
                false
            )
        else
            getfield(
                computeLabelOwner(languageVersionSettings, classBuilderForCoroutineState.thisName).internalName,
                COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor
            )
    }

    private fun InstructionAdapter.setLabel() {
        if (isForNamedFunction && !languageVersionSettings.isReleaseCoroutines())
            invokevirtual(
                classBuilderForCoroutineState.thisName,
                "setLabel",
                Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE),
                false
            )
        else
            putfield(
                computeLabelOwner(languageVersionSettings, classBuilderForCoroutineState.thisName).internalName,
                COROUTINE_LABEL_FIELD_NAME, Type.INT_TYPE.descriptor
            )
    }

    private fun updateMaxStack(methodNode: MethodNode) {
        methodNode.instructions.resetLabels()
        methodNode.accept(
            MaxStackFrameSizeAndLocalsCalculator(
                Opcodes.API_VERSION, methodNode.access, methodNode.desc,
                object : MethodVisitor(Opcodes.API_VERSION) {
                    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
                        methodNode.maxStack = maxStack
                    }
                }
            )
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
                classBuilderForCoroutineState,
                languageVersionSettings
            )

            visitVarInsn(Opcodes.ASTORE, continuationIndex)

            visitLabel(afterCoroutineStateCreated)

            visitVarInsn(Opcodes.ALOAD, continuationIndex)
            getfield(classBuilderForCoroutineState.thisName, languageVersionSettings.dataFieldName(), AsmTypes.OBJECT_TYPE.descriptor)
            visitVarInsn(Opcodes.ASTORE, dataIndex)

            val resultStartLabel = Label()
            visitLabel(resultStartLabel)

            addContinuationAndResultToLvt(methodNode, afterCoroutineStateCreated, resultStartLabel)

            if (!languageVersionSettings.isReleaseCoroutines()) {
                visitVarInsn(Opcodes.ALOAD, continuationIndex)
                getfield(classBuilderForCoroutineState.thisName, EXCEPTION_FIELD_NAME, AsmTypes.JAVA_THROWABLE_TYPE.descriptor)
                visitVarInsn(Opcodes.ASTORE, exceptionIndex)
            }
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

        val starts = methodNode.instructions.asSequence().filter {
            isBeforeSuspendMarker(it) &&
                    cfg.getPredecessorsIndices(it).isNotEmpty() // Ignore unreachable start markers
        }.toList()
        return starts.mapNotNull { start ->
            val ends = mutableSetOf<AbstractInsnNode>()
            if (collectSuspensionPointEnds(start, mutableSetOf(), ends)) return@mapNotNull null
            // Ignore suspension points, if the suspension call begin is alive and suspension call end is dead
            // (e.g., an inlined suspend function call ends with throwing a exception -- see KT-15017),
            // (also see boxInline/suspend/stateMachine/unreachableSuspendMarker.kt)
            // this is an exit point for the corresponding coroutine.
            val end = ends.find { isAfterSuspendMarker(it) } ?: return@mapNotNull null
            SuspensionPoint(start.previous, end)
        }
    }

    private fun dropSuspensionMarkers(methodNode: MethodNode) {
        // Drop markers, including ones, which we ignored in recognizing phase
        for (marker in methodNode.instructions.asSequence().filter { isBeforeSuspendMarker(it) || isAfterSuspendMarker(it) }.toList()) {
            methodNode.instructions.removeAll(listOf(marker.previous, marker))
        }
    }

    private fun spillVariables(suspensionPoints: List<SuspensionPoint>, methodNode: MethodNode): List<List<SpilledVariableDescriptor>> {
        val instructions = methodNode.instructions
        val frames = performRefinedTypeAnalysis(methodNode, containingClassInternalName)
        fun AbstractInsnNode.index() = instructions.indexOf(this)

        // We postpone these actions because they change instruction indices that we use when obtaining frames
        val postponedActions = mutableListOf<() -> Unit>()
        val maxVarsCountByType = mutableMapOf<Type, Int>()
        val livenessFrames = analyzeLiveness(methodNode)
        val spilledToVariableMapping = arrayListOf<List<SpilledVariableDescriptor>>()

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

            val spilledToVariable = arrayListOf<SpilledVariableDescriptor>()

            // 0 - this
            // 1 - parameter
            // ...
            // k - continuation
            // k + 1 - data
            // k + 2 - exception
            val variablesToSpill =
                (0 until localsCount)
                    .filterNot { it in setOf(continuationIndex, dataIndex, exceptionIndex) }
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
                localVariableName(methodNode, index, suspension.suspensionCallEnd.next.index())
                    ?.let { spilledToVariable.add(SpilledVariableDescriptor(fieldName, it)) }

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

            spilledToVariableMapping.add(spilledToVariable)

            varsCountByType.forEach {
                maxVarsCountByType[it.key] = max(maxVarsCountByType[it.key] ?: 0, it.value)
            }
        }

        postponedActions.forEach(Function0<Unit>::invoke)

        maxVarsCountByType.forEach { entry ->
            val (type, maxIndex) = entry
            for (index in 0..maxIndex) {
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
                "Next instruction after ${this} should be a label, but " +
                        "${suspensionCallEnd.next::class.java}/${suspensionCallEnd.next.opcode} was found"
            }

            return suspensionCallEnd.next as LabelNode
        }

    private fun transformCallAndReturnContinuationLabel(
        id: Int,
        suspension: SuspensionPoint,
        methodNode: MethodNode,
        suspendMarkerVarIndex: Int,
        suspendPointLineNumber: LineNumberNode?
    ): LabelNode {
        val continuationLabel = LabelNode()
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
                visitLabel(continuationLabel.label)
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
                generateResumeWithExceptionCheck(languageVersionSettings.isReleaseCoroutines(), dataIndex, exceptionIndex)

                // Load continuation argument just like suspending function returns it
                load(dataIndex, AsmTypes.OBJECT_TYPE)

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

        return continuationLabel
    }

    // Find the next line number instruction that is defintely hit. That is, a line number
    // that comes before any branch or method call.
    private fun nextDefinitelyHitLineNumber(suspension: SuspensionPoint): LineNumberNode? {
        var next = suspension.suspensionCallEnd.next
        while (next != null) {
            if (next.isBranchOrCall) return null
            else if (next is LineNumberNode) return next
            else next = next.next
        }
        return next
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

    private data class SpilledVariableDescriptor(val fieldName: String, val variableName: String)
}

// TODO Use this in variable liveness analysis
private class MethodNodeExaminer(
    val languageVersionSettings: LanguageVersionSettings,
    val containingClassInternalName: String,
    val methodNode: MethodNode,
    disableTailCallOptimizationForFunctionReturningUnit: Boolean
) {
    private val sourceFrames: Array<Frame<SourceValue>?> =
        MethodTransformer.analyze(containingClassInternalName, methodNode, IgnoringCopyOperationSourceInterpreter())
    private val controlFlowGraph = ControlFlowGraph.build(methodNode)

    private val safeUnitInstances = mutableSetOf<AbstractInsnNode>()
    private val popsBeforeSafeUnitInstances = mutableSetOf<AbstractInsnNode>()
    private val areturnsAfterSafeUnitInstances = mutableSetOf<AbstractInsnNode>()
    private val meaningfulSuccessorsCache = hashMapOf<AbstractInsnNode, List<AbstractInsnNode>>()
    private val meaningfulPredecessorsCache = hashMapOf<AbstractInsnNode, List<AbstractInsnNode>>()

    init {
        if (!disableTailCallOptimizationForFunctionReturningUnit) {
            // retrieve all POP insns
            val pops = methodNode.instructions.asSequence().filter { it.opcode == Opcodes.POP }
            // for each of them check that all successors are PUSH Unit
            val popsBeforeUnitInstances = pops.map { it to it.meaningfulSuccessors() }
                .filter { (_, succs) -> succs.all { it.isUnitInstance() } }
                .map { it.first }.toList()
            for (pop in popsBeforeUnitInstances) {
                val units = pop.meaningfulSuccessors()
                val allUnitsAreSafe = units.all { unit ->
                    // check no other predecessor exists
                    unit.meaningfulPredecessors().all { it in popsBeforeUnitInstances } &&
                            // check they have only returns among successors
                            unit.meaningfulSuccessors().all { it.opcode == Opcodes.ARETURN }
                }
                if (!allUnitsAreSafe) continue
                // save them all to the properties
                popsBeforeSafeUnitInstances += pop
                safeUnitInstances += units
                units.flatMapTo(areturnsAfterSafeUnitInstances) { it.meaningfulSuccessors() }
            }
        }
    }

    private fun AbstractInsnNode.index() = methodNode.instructions.indexOf(this)

    // GETSTATIC kotlin/Unit.INSTANCE is considered safe iff
    // it is part of POP, PUSH Unit, ARETURN sequence.
    private fun AbstractInsnNode.isSafeUnitInstance(): Boolean = this in safeUnitInstances

    private fun AbstractInsnNode.isPopBeforeSafeUnitInstance(): Boolean = this in popsBeforeSafeUnitInstances
    private fun AbstractInsnNode.isAreturnAfterSafeUnitInstance(): Boolean = this in areturnsAfterSafeUnitInstances

    private fun AbstractInsnNode.meaningfulSuccessors(): List<AbstractInsnNode> = meaningfulSuccessorsCache.getOrPut(this) {
        meaningfulSuccessorsOrPredecessors(true)
    }

    private fun AbstractInsnNode.meaningfulPredecessors(): List<AbstractInsnNode> = meaningfulPredecessorsCache.getOrPut(this) {
        meaningfulSuccessorsOrPredecessors(false)
    }

    private fun AbstractInsnNode.meaningfulSuccessorsOrPredecessors(isSuccessors: Boolean): List<AbstractInsnNode> {
        fun AbstractInsnNode.isMeaningful() = isMeaningful && opcode != Opcodes.NOP && opcode != Opcodes.GOTO && this !is LineNumberNode

        fun AbstractInsnNode.getIndices() =
            if (isSuccessors) controlFlowGraph.getSuccessorsIndices(this)
            else controlFlowGraph.getPredecessorsIndices(this)

        val visited = arrayListOf<AbstractInsnNode>()
        fun dfs(insn: AbstractInsnNode) {
            if (insn in visited) return
            visited += insn
            if (!insn.isMeaningful()) {
                for (succIndex in insn.getIndices()) {
                    dfs(methodNode.instructions[succIndex])
                }
            }
        }

        for (succIndex in getIndices()) {
            dfs(methodNode.instructions[succIndex])
        }
        return visited.filter { it.isMeaningful() }
    }

    fun replacePopsBeforeSafeUnitInstancesWithCoroutineSuspendedChecks() {
        val basicAnalyser = Analyzer(BasicInterpreter())
        basicAnalyser.analyze(containingClassInternalName, methodNode)
        val typedFrames = basicAnalyser.frames

        val isReferenceMap = popsBeforeSafeUnitInstances
            .map { it to (!isUnreachable(it.index(), sourceFrames) && typedFrames[it.index()]?.top()?.isReference == true) }
            .toMap()

        for (pop in popsBeforeSafeUnitInstances) {
            if (isReferenceMap[pop] == true) {
                val label = Label()
                methodNode.instructions.insertBefore(pop, withInstructionAdapter {
                    dup()
                    loadCoroutineSuspendedMarker(languageVersionSettings)
                    ifacmpne(label)
                    areturn(AsmTypes.OBJECT_TYPE)
                    mark(label)
                })
            }
        }
    }

    fun allSuspensionPointsAreTailCalls(suspensionPoints: List<SuspensionPoint>): Boolean {
        val safelyReachableReturns = findSafelyReachableReturns()

        val instructions = methodNode.instructions
        return suspensionPoints.all { suspensionPoint ->
            val beginIndex = instructions.indexOf(suspensionPoint.suspensionCallBegin)
            val endIndex = instructions.indexOf(suspensionPoint.suspensionCallEnd)

            if (isUnreachable(endIndex, sourceFrames)) return@all true

            val insideTryBlock = methodNode.tryCatchBlocks.any { block ->
                val tryBlockStartIndex = instructions.indexOf(block.start)
                val tryBlockEndIndex = instructions.indexOf(block.end)

                beginIndex in tryBlockStartIndex..tryBlockEndIndex
            }
            if (insideTryBlock) return@all false

            safelyReachableReturns[endIndex + 1]?.all { returnIndex ->
                sourceFrames[returnIndex]?.top().sure {
                    "There must be some value on stack to return"
                }.insns.any { sourceInsn ->
                    sourceInsn?.let(instructions::indexOf) in beginIndex..endIndex
                }
            } ?: false
        }
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
    private fun findSafelyReachableReturns(): Array<Set<Int>?> {
        val insns = methodNode.instructions
        val reachableReturnsIndices = Array<Set<Int>?>(insns.size()) init@{ index ->
            val insn = insns[index]

            if (insn.opcode == Opcodes.ARETURN && !insn.isAreturnAfterSafeUnitInstance()) {
                if (isUnreachable(index, sourceFrames)) return@init null
                return@init setOf(index)
            }

            // Since POP, PUSH Unit, ARETURN behaves like normal return in terms of tail-call optimization, set return index to POP
            if (insn.isPopBeforeSafeUnitInstance()) {
                return@init setOf(index)
            }

            if (!insn.isMeaningful || insn.opcode in SAFE_OPCODES || insn.isInvisibleInDebugVarInsn(methodNode) || isInlineMarker(insn)
                || insn.isSafeUnitInstance() || insn.isAreturnAfterSafeUnitInstance()
            ) {
                setOf<Int>()
            } else null
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
}

internal fun InstructionAdapter.generateContinuationConstructorCall(
    objectTypeForState: Type?,
    methodNode: MethodNode,
    needDispatchReceiver: Boolean,
    internalNameForDispatchReceiver: String?,
    containingClassInternalName: String,
    classBuilderForCoroutineState: ClassBuilder,
    languageVersionSettings: LanguageVersionSettings
) {
    anew(objectTypeForState)
    dup()

    val parameterTypesAndIndices =
        getParameterTypesIndicesForCoroutineConstructor(
            methodNode.desc,
            methodNode.access,
            needDispatchReceiver, internalNameForDispatchReceiver ?: containingClassInternalName,
            languageVersionSettings
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

private fun InstructionAdapter.generateResumeWithExceptionCheck(isReleaseCoroutines: Boolean, dataIndex: Int, exceptionIndex: Int) {
    // Check if resumeWithException has been called

    if (isReleaseCoroutines) {
        load(dataIndex, AsmTypes.OBJECT_TYPE)
        invokestatic("kotlin/ResultKt", "throwOnFailure", "(Ljava/lang/Object;)V", false)
    } else {
        load(exceptionIndex, AsmTypes.OBJECT_TYPE)
        dup()
        val noExceptionLabel = Label()
        ifnull(noExceptionLabel)
        athrow()

        mark(noExceptionLabel)
        pop()
    }
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
}

internal fun getLastParameterIndex(desc: String, access: Int) =
    Type.getArgumentTypes(desc).dropLast(1).map { it.size }.sum() + (if (!isStatic(access)) 1 else 0)

private fun getParameterTypesForCoroutineConstructor(desc: String, hasDispatchReceiver: Boolean, thisName: String) =
    listOfNotNull(if (!hasDispatchReceiver) null else Type.getObjectType(thisName)).toTypedArray() +
            Type.getArgumentTypes(desc).last()

private fun isStatic(access: Int) = access and Opcodes.ACC_STATIC != 0

private fun getParameterTypesIndicesForCoroutineConstructor(
    desc: String,
    containingFunctionAccess: Int,
    needDispatchReceiver: Boolean,
    thisName: String,
    languageVersionSettings: LanguageVersionSettings
): Collection<Pair<Type, Int>> {
    return mutableListOf<Pair<Type, Int>>().apply {
        if (needDispatchReceiver) {
            add(Type.getObjectType(thisName) to 0)
        }
        val continuationIndex =
            getAllParameterTypes(desc, !isStatic(containingFunctionAccess), thisName).dropLast(1).map(Type::getSize).sum()
        add(languageVersionSettings.continuationAsmType() to continuationIndex)
    }
}

private fun getAllParameterTypes(desc: String, hasDispatchReceiver: Boolean, thisName: String) =
    listOfNotNull(if (!hasDispatchReceiver) null else Type.getObjectType(thisName)).toTypedArray() +
            Type.getArgumentTypes(desc)

internal class IgnoringCopyOperationSourceInterpreter : SourceInterpreter(Opcodes.API_VERSION) {
    override fun copyOperation(insn: AbstractInsnNode?, value: SourceValue?) = value
}

// Check whether this instruction is unreachable, i.e. there is no path leading to this instruction
internal fun <T : Value> isUnreachable(index: Int, sourceFrames: Array<out Frame<out T>?>): Boolean =
    sourceFrames.size <= index || sourceFrames[index] == null

private fun AbstractInsnNode?.isInvisibleInDebugVarInsn(methodNode: MethodNode): Boolean {
    val insns = methodNode.instructions
    val index = insns.indexOf(this)
    return (this is VarInsnNode && methodNode.localVariables.none {
        it.index == `var` && index in it.start.let(insns::indexOf)..it.end.let(insns::indexOf)
    })
}

private val SAFE_OPCODES =
    ((Opcodes.DUP..Opcodes.DUP2_X2) + Opcodes.NOP + Opcodes.POP + Opcodes.POP2 + (Opcodes.IFEQ..Opcodes.GOTO)).toSet()

internal fun replaceFakeContinuationsWithRealOnes(methodNode: MethodNode, continuationIndex: Int) {
    val fakeContinuations = methodNode.instructions.asSequence().filter(::isFakeContinuationMarker).toList()
    for (fakeContinuation in fakeContinuations) {
        methodNode.instructions.removeAll(listOf(fakeContinuation.previous.previous, fakeContinuation.previous))
        methodNode.instructions.set(fakeContinuation, VarInsnNode(Opcodes.ALOAD, continuationIndex))
    }
}
