/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.optimization.common.*
import org.jetbrains.kotlin.codegen.optimization.fixStack.FixStackMethodTransformer
import org.jetbrains.kotlin.codegen.state.JvmBackendConfig
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import kotlin.math.max

private const val COROUTINES_DEBUG_METADATA_VERSION = 2

private const val COROUTINES_METADATA_SOURCE_FILE_JVM_NAME = "f"
private const val COROUTINES_METADATA_LINE_NUMBERS_JVM_NAME = "l"
private const val COROUTINES_METADATA_NEXT_LINE_NUMBERS_JVM_NAME = "nl"
private const val COROUTINES_METADATA_LOCAL_NAMES_JVM_NAME = "n"
private const val COROUTINES_METADATA_SPILLED_JVM_NAME = "s"
private const val COROUTINES_METADATA_INDEX_TO_LABEL_JVM_NAME = "i"
private const val COROUTINES_METADATA_METHOD_NAME_JVM_NAME = "m"
private const val COROUTINES_METADATA_CLASS_NAME_JVM_NAME = "c"
private const val COROUTINES_METADATA_VERSION_JVM_NAME = "v"

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
    private val config: JvmBackendConfig,
    // It's only matters for named functions, may differ from '!isStatic(access)' in case of DefaultImpls
    private val needDispatchReceiver: Boolean = false,
    // May differ from containingClassInternalName in case of DefaultImpls
    private val internalNameForDispatchReceiver: String? = null,
    // Parameters of suspend lambda are put to the same fields as spilled variables
    private val initialVarsCountByType: Map<Type, Int> = emptyMap(),
) : TransformationMethodVisitor(delegate, access, name, desc, signature, exceptions) {

    private val classBuilderForCoroutineState: ClassBuilder by lazy(obtainClassBuilderForCoroutineState)

    private var continuationIndex = if (isForNamedFunction) -1 else 0
    private var dataIndex = if (isForNamedFunction) -1 else 1

    private var generatedCodeMarkers: GeneratedCodeMarkers? = null

    override fun performTransformations(methodNode: MethodNode) {
        if (config.enhancedCoroutinesDebugging) {
            generatedCodeMarkers = GeneratedCodeMarkers.fillOutMarkersAndCleanUpMethodNode(methodNode)
        }

        removeFakeContinuationConstructorCall(methodNode)

        replaceReturnsUnitMarkersWithPushingUnitOnStack(methodNode)

        replaceFakeContinuationsWithRealOnes(
            methodNode,
            if (isForNamedFunction) getLastParameterIndex(methodNode.desc, methodNode.access) else 0
        )

        // If there are in-place argument and call markers around suspend call, they end up in separate
        // states of state-machine, leading to AnalyzerError.
        InplaceArgumentsMethodTransformer().transform(containingClassInternalName, methodNode)
        FixStackMethodTransformer().transform(containingClassInternalName, methodNode)
        val suspensionPoints = collectSuspensionPoints(methodNode)
        RedundantLocalsEliminationMethodTransformer(suspensionPoints)
            .transform(containingClassInternalName, methodNode)
        ChangeBoxingMethodTransformer.transform(containingClassInternalName, methodNode)
        methodNode.updateMaxStack()

        checkForSuspensionPointInsideMonitor(methodNode, suspensionPoints)

        // Because we add a sprinkle of fictitious line numbers, it is important not to lose correct linenumbers for suspension points.
        addLineNumberForSuspensionPointsAtTheSameLine(methodNode, suspensionPoints)

        // First instruction in the method node is different for named functions and for lambdas
        //   For named functions, before the first instruction we have continuation check
        //   For lambdas, we have lambda arguments unspilling
        var actualCoroutineStart = methodNode.instructions.first

        if (isForNamedFunction) {
            if (methodNode.allSuspensionPointsAreTailCalls(suspensionPoints, !disableTailCallOptimizationForFunctionReturningUnit)) {
                methodNode.addCoroutineSuspendedChecks(suspensionPoints)
                dropSuspensionMarkers(methodNode)
                dropUnboxInlineClassMarkers(methodNode, suspensionPoints)
                return
            }

            dataIndex = methodNode.maxLocals++
            continuationIndex = methodNode.maxLocals++

            prepareMethodNodePreludeForNamedFunction(methodNode)
        } else if (config.nullOutSpilledCoroutineLocalsUsingStdlibFunction) {
            actualCoroutineStart = methodNode.instructions.findLast { isSuspendLambdaParameterMarker(it) }?.next ?: actualCoroutineStart
        }

        if (!isForNamedFunction) {
            markFakeLineNumberForLambdaArgumentUnspilling(methodNode)
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
        val suspensionPointNextLineNumbers = suspensionPoints.map { findSuspensionPointNextLineNumber(it) }

        // Create states in state-machine, to which state-machine can jump
        val stateLabels = suspensionPoints.withIndex().map {
            transformCallAndReturnStateLabel(
                it.index + 1, it.value, methodNode, suspendMarkerVarIndex, suspensionPointLineNumbers[it.index]
            )
        }

        generateStateMachinesTableswitch(methodNode, actualCoroutineStart, suspendMarkerVarIndex, suspensionPoints, stateLabels)

        initializeFakeInlinerVariables(methodNode, stateLabels)

        dropSuspensionMarkers(methodNode)
        dropUnboxInlineClassMarkers(methodNode, suspensionPoints)
        methodNode.removeEmptyCatchBlocks()
        if (config.nullOutSpilledCoroutineLocalsUsingStdlibFunction) {
            methodNode.extendParameterRanges()
            methodNode.extendSuspendLambdaParameterRanges()
        }
        dropSuspendLambdaParameterMarkers(methodNode)

        if (!config.nullOutSpilledCoroutineLocalsUsingStdlibFunction && !config.enableDebugMode) {
            updateLvtAccordingToLiveness(methodNode, isForNamedFunction, stateLabels)
        }

        generatedCodeMarkers?.addFakeVariablesToLVTAndInitializeThem(methodNode, isForNamedFunction)

        writeDebugMetadata(methodNode, suspensionPointLineNumbers, suspensionPointNextLineNumbers, spilledToVariableMapping)
    }

    private fun addLineNumberForSuspensionPointsAtTheSameLine(node: MethodNode, points: List<SuspensionPoint>) {
        for (i in points.dropLast(1).indices) {
            if (InsnSequence(points[i].suspensionCallEnd.next, points[i+1].suspensionCallBegin).none { it is LineNumberNode }) {
                val lineNumber = findSuspensionPointLineNumber(points[i])
                if (lineNumber != null) {
                    node.instructions.insertBefore(points[i+1].suspensionCallBegin, withInstructionAdapter {
                        val label = Label()
                        mark(label)
                        visitLineNumber(lineNumber.line, label)
                    })
                }
            }
        }
    }

    private fun generateStateMachinesTableswitch(
        methodNode: MethodNode,
        actualCoroutineStart: AbstractInsnNode?,
        suspendMarkerVarIndex: Int,
        suspensionPoints: List<SuspensionPoint>,
        stateLabels: List<LabelNode>,
    ) {
        methodNode.instructions.apply {
            val tableSwitchLabel = LabelNode()
            val firstStateLabel = LabelNode()
            val defaultLabel = LabelNode()

            insertBefore(actualCoroutineStart, withInstructionAdapter {
                GeneratedCodeMarkers.markFakeLineNumber(this, generatedCodeMarkers?.tableswitch)
            })

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

            // Insert throw of an IllegalStateException if the resumption point is unknown. This code does not correspond to
            // anything in the input code. We give it the entry line number instead of letting it inherit the line number
            // of the last branch to avoid debugger issues where reordering the blocks leads to inability to set a breakpoint
            // on the last expression in a suspend function. See KT-51936 for a concrete example.
            //
            // The IntelliJ debugger tries to avoid stuttering by only setting a breakpoint on the first bytecode offset that
            // corresponds to a line number. Therefore, if the code is generated as:
            //
            //    line 1: switch
            //    line 2: case 1: ...
            //    line 3:         ...
            //    line 4: case 2: ...
            //    line 5:         ...
            //            default: throw IllegalStateException
            //
            // The default case ends up with line number 5. Now, compilers could (and the D8 dexer someties does) reorder
            // the blocks for the cases to end up with:
            //
            //    line 1: switch
            //    line 5: default : throw IllegalStateException
            //    line 2: case 1: ...
            //    line 3:         ...
            //    line 4: case 2: ...
            //    line 5:         ...
            //
            // This is equivalent to the original code. However, if the user tries to set a breakpoint on line 5, it will
            // ONLY be set on the throw of the IllegalStateException and not in the actual user code in case 2. And therefore
            // it is impossible for a developer to hit a breakpoint on the last line of a suspend function.
            //
            // Using line 1 for the default case limits this issue as the entry block is rarely (if ever) reordered.
            insert(last, defaultLabel)
            insert(last, LineNumberNode(lineNumber, defaultLabel))

            insert(last, withInstructionAdapter {
                GeneratedCodeMarkers.markFakeLineNumber(this, generatedCodeMarkers?.unreachable)
            })

            insert(last, withInstructionAdapter {
                AsmUtil.genThrow(this, "java/lang/IllegalStateException", ILLEGAL_STATE_ERROR_MESSAGE)
                areturn(Type.VOID_TYPE)
            })
        }
    }

    private fun markFakeLineNumberForLambdaArgumentUnspilling(node: MethodNode) {
        val label = node.instructions.find { isSuspendLambdaParameterMarker(it) }?.findPreviousOrNull { it is LabelNode }
        if (label == null) return
        node.instructions.insert(label, withInstructionAdapter {
            GeneratedCodeMarkers.markFakeLineNumber(this, generatedCodeMarkers?.lambdaArgumentsUnspilling)
        })
    }

    // When suspension point is inlined, it is in range of fake inliner variables.
    // Path from TABLESWITCH into unspilling goes to latter part of the range.
    // In this case the variables are uninitialized, initialize them, and split the local variable
    // range so that the local variable is only defined when initialized.
    private fun initializeFakeInlinerVariables(methodNode: MethodNode, stateLabels: List<LabelNode>) {
        for (stateLabel in stateLabels) {
            val newRecords = mutableListOf<LocalVariableNode>()
            for (record in methodNode.localVariables) {
                if (JvmAbi.isFakeLocalVariableForInline(record.name) &&
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

    private fun findSuspensionPointLineNumber(suspensionPoint: SuspensionPoint): LineNumberNode? =
        suspensionPoint.suspensionCallBegin.findPreviousOrNull { it is LineNumberNode } as LineNumberNode?

    private fun findSuspensionPointNextLineNumber(suspensionPoint: SuspensionPoint): LineNumberNode? =
        suspensionPoint.suspensionCallEnd.findNextOrNull { it is LineNumberNode } as LineNumberNode?

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
        suspensionPointNextLineNumbers: List<LineNumberNode?>,
        spilledToLocalMapping: List<List<SpilledVariableAndField>>
    ) {
        val lines = suspensionPointLineNumbers.map { it?.line ?: -1 }
        val nextLines = suspensionPointNextLineNumbers.map { it?.line ?: -1 }
        val metadata = classBuilderForCoroutineState.newAnnotation(DEBUG_METADATA_ANNOTATION_ASM_TYPE.descriptor, true)
        metadata.visit(COROUTINES_METADATA_SOURCE_FILE_JVM_NAME, sourceFile)
        metadata.visit(COROUTINES_METADATA_LINE_NUMBERS_JVM_NAME, lines.toIntArray())
        metadata.visit(COROUTINES_METADATA_NEXT_LINE_NUMBERS_JVM_NAME, nextLines.toIntArray())

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
        metadata.visit(COROUTINES_METADATA_VERSION_JVM_NAME, COROUTINES_DEBUG_METADATA_VERSION)
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

            GeneratedCodeMarkers.markFakeLineNumber(this, generatedCodeMarkers?.checkContinuation)

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

    private fun dropSuspendLambdaParameterMarkers(methodNode: MethodNode) {
        for (marker in methodNode.instructions.asSequence().filter { isSuspendLambdaParameterMarker(it) }.toList()) {
            methodNode.instructions.removeAll(listOf(marker.previous, marker))
        }
    }

    /**
     * Main logic here: A variable can be either alive or dead, and it can be visible by debugger or invisible
     *
     * We have to spill all visible variables - that include alive variables.
     * However, there can be dead visible variables - we spill nulls into continuation, using probe from stdlib.
     * When a variable becomes dead, we have to clean it up - spilling null into continuation.
     */
    private fun spillVariables(suspensionPoints: List<SuspensionPoint>, methodNode: MethodNode): List<List<SpilledVariableAndField>> {
        val frames: Array<out Frame<BasicValue>?> = performSpilledVariableFieldTypesAnalysis(methodNode, containingClassInternalName)

        val suspendLambdaParameters =
            if (config.nullOutSpilledCoroutineLocalsUsingStdlibFunction) methodNode.collectSuspendLambdaParameterSlots()
            else emptyList()

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
        val referencesToSpillBySuspensionPointIndex = mutableListOf<List<SpillableVariable>>()
        // while primitives shall not
        val primitivesToSpillBySuspensionPointIndex = mutableListOf<List<SpillableVariable>>()

        // Collect information about spillable variables, that we use to determine which variables we need to cleanup
        for (suspension in suspensionPoints) {
            val suspensionCallBegin = suspension.suspensionCallBegin
            val suspensionCallBeginIndex = methodNode.instructions.indexOf(suspensionCallBegin)

            require(frames[methodNode.instructions.indexOf(suspension.suspensionCallEnd.next)]?.stackSize == 1) {
                "Stack should be spilled before suspension call"
            }

            // This variable is used to calculate name of field.
            // TODO: Can we use slot number for it?
            val varsCountByType = mutableMapOf<Type, Int>()
            val variablesToSpill = calculateVariablesToSpill(
                methodNode, frames, livenessFrames, suspensionCallBeginIndex, varsCountByType
            )

            val (referencesToSpill, primitivesToSpill) = variablesToSpill.partition { variable ->
                variable.normalizedType == AsmTypes.OBJECT_TYPE
            }

            referencesToSpillBySuspensionPointIndex += referencesToSpill
            primitivesToSpillBySuspensionPointIndex += primitivesToSpill

            for ((type, index) in varsCountByType) {
                maxVarsCountByType[type] = max(maxVarsCountByType[type] ?: 0, index)
            }
        }

        // We have to clean up dead variables. If a variable becomes dead and invisible, we cannot be sure, that
        // it was visible before, so we put spill null, and do not unspill it.
        val referencesToCleanBySuspensionPointIndex = calculateVariablesToCleanup(
            methodNode, suspensionPoints, referencesToSpillBySuspensionPointIndex, initialSpilledVariablesCount
        )

        val spilledToVariableMapping = mapFieldNameToVariable(
            methodNode, suspensionPoints, referencesToSpillBySuspensionPointIndex, primitivesToSpillBySuspensionPointIndex
        )

        // Mutate method node
        for (suspensionPointIndex in suspensionPoints.indices) {
            val suspension = suspensionPoints[suspensionPointIndex]
            // First, we spill and unspill alive variables as usual
            // Also, we spill and unspill null for visible dead variables
            // `generateSpillAndUnspill` calls the probe from stdlib for us.
            for (referenceToSpill in referencesToSpillBySuspensionPointIndex[suspensionPointIndex]) {
                generateSpillAndUnspill(methodNode, suspension, referenceToSpill, suspendLambdaParameters)
            }

            // Then, we cleanup invisible dead variables
            val (currentSpilledCount, predSpilledCount) = referencesToCleanBySuspensionPointIndex[suspensionPointIndex]
            if (predSpilledCount > currentSpilledCount) {
                for (fieldIndex in currentSpilledCount until predSpilledCount) {
                    cleanUpField(methodNode, suspension, fieldIndex)
                }
            }

            for (primitiveToSpill in primitivesToSpillBySuspensionPointIndex[suspensionPointIndex]) {
                generateSpillAndUnspill(methodNode, suspension, primitiveToSpill, suspendLambdaParameters)
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

    private fun generateSpillAndUnspill(
        methodNode: MethodNode,
        suspension: SuspensionPoint,
        spillableVariable: SpillableVariable,
        suspendLambdaParameters: List<Int>
    ) {
        val local: LocalVariableNode? =
            findLocalCorrespondingToSpillableVariable(
                methodNode,
                spillableVariable,
                suspension,
                suspension.tryCatchBlockEndLabelAfterSuspensionCall
            )

        val localRestart = LabelNode().linkWithLabel()

        if (spillableVariable.isNull) {
            with(methodNode.instructions) {
                insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                    aconst(null)
                    store(spillableVariable.slot, AsmTypes.OBJECT_TYPE)
                    if (local != null) {
                        visitLabel(localRestart.label)
                    }
                })
            }
            splitLvtRecord(methodNode, suspension, local, localRestart)
            return
        }

        with(methodNode.instructions) {
            // store variable before suspension call
            // skip restoring this
            val isInstanceThisVariable = !isStatic(methodNode.access) && spillableVariable.slot == 0
            if (!isInstanceThisVariable) {
                insertBefore(suspension.suspensionCallBegin, withInstructionAdapter {
                    load(continuationIndex, AsmTypes.OBJECT_TYPE)

                    if (!config.enableDebugMode && spillableVariable.shouldSpillNull) {
                        if (config.nullOutSpilledCoroutineLocalsUsingStdlibFunction) {
                            putOnStack(spillableVariable)
                            invokeNullOutSpilledVariable()
                        } else {
                            aconst(null)
                        }
                    } else {
                        putOnStack(spillableVariable)
                    }

                    putfield(
                        classBuilderForCoroutineState.thisName,
                        spillableVariable.fieldName,
                        spillableVariable.normalizedType.descriptor
                    )
                })
            }

            if (spillableVariable.slot !in suspendLambdaParameters && !isInstanceThisVariable) {
                // restore variable after suspension call
                insert(suspension.tryCatchBlockEndLabelAfterSuspensionCall, withInstructionAdapter {
                    load(continuationIndex, AsmTypes.OBJECT_TYPE)
                    getfield(
                        classBuilderForCoroutineState.thisName,
                        spillableVariable.fieldName,
                        spillableVariable.normalizedType.descriptor
                    )
                    StackValue.coerce(spillableVariable.normalizedType, spillableVariable.type, this)
                    store(spillableVariable.slot, spillableVariable.type)
                    if (local != null) {
                        visitLabel(localRestart.label)
                    }
                })

                splitLvtRecord(methodNode, suspension, local, localRestart)
            }
        }
    }

    private fun InstructionAdapter.putOnStack(spillableVariable: SpillableVariable) {
        load(spillableVariable.slot, spillableVariable.type)
        StackValue.coerce(spillableVariable.type, spillableVariable.normalizedType, this)
    }

    private fun findLocalCorrespondingToSpillableVariable(
        methodNode: MethodNode,
        spillableVariable: SpillableVariable,
        suspension: SuspensionPoint,
        tryCatchBlockEndLabelAfterSuspensionCall: LabelNode,
    ): LocalVariableNode? {
        // Find and remove the local variable node, if any, in the local variable table corresponding to the slot that is spilled.
        val iterator = methodNode.localVariables.listIterator()
        while (iterator.hasNext()) {
            val node = iterator.next()
            if (node.index == spillableVariable.slot &&
                methodNode.instructions.indexOf(node.start) <= methodNode.instructions.indexOf(suspension.suspensionCallBegin) &&
                methodNode.instructions.indexOf(node.end) > methodNode.instructions.indexOf(tryCatchBlockEndLabelAfterSuspensionCall)
            ) {
                return node
            }
        }
        return null
    }

    private fun splitLvtRecord(
        methodNode: MethodNode,
        suspension: SuspensionPoint,
        local: LocalVariableNode?,
        localRestart: LabelNode,
    ) {
        // Split the local variable range for the local so that it is visible until the next state label, but is
        // not visible until it has been unspilled from the continuation on the reentry path.
        if (local != null) {
            val previousEnd = local.end
            local.end = suspension.stateLabel
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

    private fun cleanUpField(methodNode: MethodNode, suspension: SuspensionPoint, fieldIndex: Int) {
        with(methodNode.instructions) {
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
    private fun calculateVariablesToSpill(
        methodNode: MethodNode,
        frames: Array<out Frame<BasicValue>?>,
        livenessFrames: List<VariableLivenessFrame>,
        suspensionCallBeginIndex: Int,
        varsCountByType: MutableMap<Type, Int>,
    ): MutableList<SpillableVariable> {
        val frame = frames[suspensionCallBeginIndex].sure { "Suspension points containing in dead code must be removed" }
        val localsCount = frame.locals
        val variablesToSpill = mutableListOf<SpillableVariable>()

        val livenessFrame = livenessFrames[suspensionCallBeginIndex]

        val completionSlot = getLastParameterIndex(methodNode.desc, methodNode.access)

        // 0 - this
        // 1 - parameter
        // ...
        // k - continuation
        // k + 1 - result
        for (slot in 0 until localsCount) {
            // Do not spill `this` of suspend methods
            if (!isStatic(methodNode.access) && slot == 0) continue
            // Do not spill continuation and $result
            if (slot == continuationIndex || slot == dataIndex) continue
            // Do not spill $completion
            if (isForNamedFunction && slot == completionSlot) continue
            val value = frame.getLocal(slot)
            if (value.type == null) continue
            val visibleByDebugger = methodNode.localVariables.any {
                if (it.index != slot) return@any false
                methodNode.instructions.indexOf(it.start) < suspensionCallBeginIndex &&
                        suspensionCallBeginIndex < methodNode.instructions.indexOf(it.end)
            }

            val willBeVisibleByDebugger = !livenessFrame.isAlive(slot) && !visibleByDebugger &&
                    checkWhetherVariableWillBeVisible(methodNode, slot, suspensionCallBeginIndex)

            val needToSpill = livenessFrame.isAlive(slot) ||
                    (config.nullOutSpilledCoroutineLocalsUsingStdlibFunction || config.enableDebugMode) &&
                    (visibleByDebugger || willBeVisibleByDebugger)
            if (!needToSpill) continue

            if (value === StrictBasicValue.NULL_VALUE) {
                variablesToSpill += SpillableVariable(
                    value, AsmTypes.OBJECT_TYPE, AsmTypes.OBJECT_TYPE, null, slot, visibleByDebugger, livenessFrame.isAlive(slot)
                )
                continue
            }

            val type = value.type!!
            val normalizedType = type.normalize()

            val indexBySort = varsCountByType[normalizedType]?.plus(1) ?: 0
            varsCountByType[normalizedType] = indexBySort

            val fieldName = normalizedType.fieldNameForVar(indexBySort)
            variablesToSpill += SpillableVariable(
                value, type, normalizedType, fieldName, slot, visibleByDebugger, livenessFrame.isAlive(slot)
            )
        }
        return variablesToSpill
    }

    // When suspension point is inside finally block, its LVT record is split, but there is no store for the second half
    // in that case, we need to spill the variable, since it will be visible by the debugger
    private fun checkWhetherVariableWillBeVisible(
        methodNode: MethodNode,
        slot: Int,
        suspensionCallBeginIndex: Int
    ): Boolean {
        val local =
            methodNode.localVariables.filter { it.index == slot && suspensionCallBeginIndex < methodNode.instructions.indexOf(it.start)}
                .minByOrNull { methodNode.instructions.indexOf(it.start) } ?: return false
        // Check, that we indeed reuse the variable and not introduce it
        var cursor: AbstractInsnNode? = methodNode.instructions[suspensionCallBeginIndex]
        while (cursor != null && cursor != local.start) {
            if (cursor.isStoreOperation() && (cursor as VarInsnNode).`var` == slot) {
                // This is indeed new variable, no need to spill
                return false
            }
            cursor = cursor.next
        }
        return true
    }

    private fun mapFieldNameToVariable(
        methodNode: MethodNode,
        suspensionPoints: List<SuspensionPoint>,
        referencesToSpillBySuspensionPointIndex: MutableList<List<SpillableVariable>>,
        primitivesToSpillBySuspensionPointIndex: MutableList<List<SpillableVariable>>,
    ): MutableList<List<SpilledVariableAndField>> {
        val spilledToVariableMapping = mutableListOf<List<SpilledVariableAndField>>()
        for (suspensionPointIndex in suspensionPoints.indices) {
            val suspension = suspensionPoints[suspensionPointIndex]

            val spilledToVariable = mutableListOf<SpilledVariableAndField>()

            referencesToSpillBySuspensionPointIndex[suspensionPointIndex].mapNotNullTo(spilledToVariable) { spillableVariable ->
                calculateSpilledVariableAndField(methodNode, suspension, spillableVariable)
            }
            primitivesToSpillBySuspensionPointIndex[suspensionPointIndex].mapNotNullTo(spilledToVariable) { spillableVariable ->
                calculateSpilledVariableAndField(methodNode, suspension, spillableVariable)
            }

            spilledToVariableMapping += spilledToVariable
        }
        return spilledToVariableMapping
    }

    // Calculate the number of variables spilled of each suspension point in comparison to predecessors
    // Return current to pred.
    private fun calculateVariablesToCleanup(
        methodNode: MethodNode,
        suspensionPoints: List<SuspensionPoint>,
        referencesToSpillBySuspensionPointIndex: List<List<SpillableVariable>>,
        initialSpilledVariablesCount: Int,
    ): MutableList<Pair<Int, Int>> {
        val predSuspensionPoints = calculateSuspensionPointPredecessorsMapping(methodNode, suspensionPoints)

        val referencesToCleanBySuspensionPointIndex = mutableListOf<Pair<Int, Int>>() // current to pred
        for (suspensionPointIndex in suspensionPoints.indices) {
            val suspensionPoint = suspensionPoints[suspensionPointIndex]
            // If we spill a variable - we are sure it is dead or visible
            val currentSpilledReferencesCount = countVariablesToSpill(referencesToSpillBySuspensionPointIndex, suspensionPointIndex)
            val preds = predSuspensionPoints[suspensionPoint]
            val predSpilledReferencesCount =
                if (preds.isNullOrEmpty()) initialSpilledVariablesCount
                else preds.maxOf { countVariablesToSpill(referencesToSpillBySuspensionPointIndex, suspensionPoints.indexOf(it)) }
            referencesToCleanBySuspensionPointIndex += currentSpilledReferencesCount to predSpilledReferencesCount
        }
        return referencesToCleanBySuspensionPointIndex
    }

    private fun calculateSuspensionPointPredecessorsMapping(
        methodNode: MethodNode,
        suspensionPoints: List<SuspensionPoint>,
    ): Map<SuspensionPoint, List<SuspensionPoint>> {
        // Use CFG to calculate amount of spilled variables in previous suspension point (P) and current one (C).
        // All fields from L$C to L$P should be cleaned. I.e. we should spill ACONST_NULL to them.
        val cfg = ControlFlowGraph.build(methodNode)

        val suspensionPointEnds: Map<AbstractInsnNode, SuspensionPoint> = suspensionPoints.associateBy { it.suspensionCallEnd }

        val predSuspensionPoints = suspensionPoints.associateWith {
            findSuspensionPointPredecessors(suspensionPointEnds, cfg, methodNode.instructions, it)
        }
        return predSuspensionPoints
    }

    // Collect all immediately preceding suspension points. I.e. suspension points, from which there is a path
    // into current one, that does not cross other suspension points.
    // TODO: Traverse CFG only once forward instead of traversing it backwards for each suspension point
    private fun findSuspensionPointPredecessors(
        suspensionPointEnds: Map<AbstractInsnNode, SuspensionPoint>,
        cfg: ControlFlowGraph,
        instructions: InsnList,
        suspension: SuspensionPoint,
    ): List<SuspensionPoint> {
        val visited = mutableSetOf<AbstractInsnNode>()
        val current = mutableListOf(suspension.suspensionCallBegin)
        val result = mutableListOf<SuspensionPoint>()

        while (current.isNotEmpty()) {
            val insn = current.popLast()
            if (!visited.add(insn)) continue

            val end = suspensionPointEnds[insn]
            if (end != null) {
                result.add(end)
                continue
            }
            current.addAll(cfg.getPredecessorsIndices(insn).map { instructions[it] })
        }

        return result
    }

    // Calculate all pairs SuspensionPoint -> C and P, where P is minimum of all preds' Cs
    private fun countVariablesToSpill(referencesToSpillBySuspensionPointIndex: List<List<SpillableVariable>>, index: Int): Int =
        referencesToSpillBySuspensionPointIndex[index].count { variable -> !variable.isNull }

    // Calculate debug metadata mapping before modifying method node to make it easier to locate
    // locals alive across suspension points.
    private fun calculateSpilledVariableAndField(
        methodNode: MethodNode,
        suspension: SuspensionPoint,
        spillableVariable: SpillableVariable
    ): SpilledVariableAndField? {
        if (spillableVariable.isNull) return null
        val name = localVariableName(methodNode, spillableVariable.slot, methodNode.instructions.indexOf(suspension.suspensionCallBegin)) ?: return null
        return SpilledVariableAndField(spillableVariable.fieldName!!, name)
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
                GeneratedCodeMarkers.markFakeLineNumber(this, generatedCodeMarkers?.checkCOROUTINE_SUSPENDED)

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

    private fun InstructionAdapter.generateResumeWithExceptionCheck(dataIndex: Int) {
        // Check if resumeWithException has been called

        GeneratedCodeMarkers.markFakeLineNumber(this, generatedCodeMarkers?.checkResult)

        load(dataIndex, AsmTypes.OBJECT_TYPE)
        invokestatic("kotlin/ResultKt", "throwOnFailure", "(Ljava/lang/Object;)V", false)
    }

    private data class SpilledVariableAndField(val fieldName: String, val variableName: String)
}

private fun MethodNode.collectSuspendLambdaParameterSlots(): List<Int> {
    return instructions.filter { isSuspendLambdaParameterMarker(it) }
        .mapNotNull { (it?.previous?.previous as? VarInsnNode)?.`var` }.toList()
}

private fun MethodNode.extendSuspendLambdaParameterRanges() {
    val slots = collectSuspendLambdaParameterSlots()
    if (slots.isEmpty()) return
    val startLabel = instructions.findLast { isSuspendLambdaParameterMarker(it) }
        ?.findNextOrNull { it is LabelNode } as? LabelNode ?: return
    for (slot in slots) {
        val duplicates = localVariables.filter { it.index == slot }
        val toExtend = duplicates.firstOrNull() ?: continue
        localVariables.removeAll(duplicates)
        toExtend.start = startLabel
        toExtend.end = getOrCreateEndingLabel()
        localVariables.add(toExtend)
    }
}

private class SpillableVariable(
    val value: BasicValue,
    val type: Type,
    val normalizedType: Type,
    val fieldName: String?,
    val slot: Int,
    // This field is for debugging purposes only - `isAlive` is enough for the algorithm to determine whether we spill
    // null or real value. We do not count invisible dead variables. Thus, this field is redundant, but useful for debugging -
    // instead of calculating whether the variable is in range of record in LVT, we just store the information.
    @Suppress("unused")
    val isVisible: Boolean,
    val isAlive: Boolean,
) {
    init {
        require(isNull == (fieldName == null)) {
            "Value is $value, fieldName is $fieldName"
        }
    }

    val isNull: Boolean
        get() = value === StrictBasicValue.NULL_VALUE

    val shouldSpillNull: Boolean
        get() = normalizedType == AsmTypes.OBJECT_TYPE && !isAlive
}

private fun InstructionAdapter.generateContinuationConstructorCall(
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

// Handy debugging routine
@Suppress("unused")
fun MethodNode.nodeTextWithVisibleVariables(): String {
    fun visibleVariables(i: Int): String {
        val res = CharArray(maxLocals)
        for (slot in 0 until maxLocals) {
            val count = localVariables.count { it.index == slot && instructions.indexOf(it.start) <= i && i < instructions.indexOf(it.end) }
            res[slot] = if (count == 0) ' ' else "$count"[0]
        }
        return String(res) + "|"
    }
    return instructions.withIndex().joinToString("\n") { (i, insn) -> "${visibleVariables(i)}${insn.insnText}" }
}

// Handy debugging routine
@Suppress("unused")
private fun MethodNode.nodeTextWithLiveness(liveness: List<VariableLivenessFrame>): String =
    liveness.zip(this.instructions.asSequence().toList()).joinToString("\n") { (a, b) -> "$a|${b.insnText}" }

/*
 * Before ApiVersion 2.2.
 * We do not want to spill dead variables, thus, we shrink its LVT record to region, where the variable is alive,
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
        var nextSuspensionPointIndex = 0
        for (insnIndex in 0 until (method.instructions.size() - 1)) {
            val insn = method.instructions[insnIndex]
            if (insn is LabelNode && nextSuspensionPointIndex < suspensionPoints.size &&
                suspensionPoints[nextSuspensionPointIndex] == insn
            ) {
                nextSuspensionPointIndex++
            }
            if (!isAlive(insnIndex, variableIndex) && isAlive(insnIndex + 1, variableIndex)) {
                startLabel = insn as? LabelNode ?: insn.findNextOrNull { it is LabelNode } as? LabelNode
            }
            if (isAlive(insnIndex, variableIndex) && !isAlive(insnIndex + 1, variableIndex)) {
                // No variable in LVT -> do not add one
                val lvtRecord = oldLvt.findRecord(insnIndex, variableIndex) ?: continue
                if (lvtRecord.name == CONTINUATION_VARIABLE_NAME ||
                    lvtRecord.name == SUSPEND_CALL_RESULT_NAME ||
                    JvmAbi.isFakeLocalVariableForInline(lvtRecord.name)
                ) continue
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
                val extended =
                    latest?.extendRecordIfPossible(method, suspensionPoints, lvtRecord.end, liveness, nextSuspensionPointIndex) ?: false
                if (!extended) {
                    val new = LocalVariableNode(lvtRecord.name, lvtRecord.desc, lvtRecord.signature, startLabel, endLabel, lvtRecord.index)
                    oldLvtNodeToLatestNewLvtNode[lvtRecord] = new
                    method.localVariables.add(new)
                    // See if we can extend it all the way to the old end.
                    new.extendRecordIfPossible(method, suspensionPoints, lvtRecord.end, liveness, nextSuspensionPointIndex)
                }
            }
        }
    }

    for (variable in oldLvt) {
        // $continuation, $completion and $result are dead, but they are used by debugger, as well as fake inliner variables
        // $continuation is used to create async stack trace
        if (variable.name == CONTINUATION_VARIABLE_NAME ||
            variable.name == SUSPEND_CALL_RESULT_NAME ||
            JvmAbi.isFakeLocalVariableForInline(variable.name)
        ) {
            method.localVariables.add(variable)
            continue
        }
        // $completion is used for stepping
        if (variable.name == SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME) {
            // There can be multiple $completion variables because of inlining, do not duplicate them
            if (method.localVariables.any { it.name == SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME }) continue

            method.extendCompletionsRange(variable, getLastParameterIndex(method.desc, method.access))
            continue
        }
        // this acts like $continuation for lambdas. For example, it is used by debugger to create async stack trace. Keep it.
        if (variable.name == "this" && !isForNamedFunction) {
            method.extendCompletionsRange(variable, 0)
            continue
        }
    }
}

// $completion should behave like ordinary parameter - span the whole function,
// unlike other parameters, it is safe to do so, since it always will have some value
// either completion (when there was no suspension) or continuation, when there was suspension.
// It is OK to have this discrepancy, since debugger walks through completion chain and to them
// there is no difference whether there is an additional link in the chain.
//
// The same applies for suspend lambdas and `this`, but there is no additional link in the chain.
private fun MethodNode.extendCompletionsRange(completion: LocalVariableNode, slot: Int) {
    completion.start = getOrCreateStartingLabel()
    completion.end = getOrCreateEndingLabel()
    completion.index = slot
    localVariables.add(completion)
}

// Parameters should behave like ordinary parameter - span the whole function,
// however, building a state-machine changes starting and ending labels.
// Fix them up.
private fun MethodNode.extendParameterRanges() {
    val toDelete = mutableSetOf<LocalVariableNode>()
    val startingLabel = getOrCreateStartingLabel()
    val endingLabel = getOrCreateEndingLabel()
    for (slot in 0..getLastParameterIndex(desc, access)) {
        val variable = localVariables.firstOrNull { it.index == slot } ?: continue
        variable.start = startingLabel
        variable.end = endingLabel
        toDelete += localVariables.filter { it.index == slot && it != variable }
    }
    localVariables.removeAll(toDelete)
}

fun MethodNode.getOrCreateStartingLabel(): LabelNode {
    val first = instructions.first
    if (first is LabelNode) return first
    val result = LabelNode()
    instructions.insertBefore(first, result)
    return result
}

fun MethodNode.getOrCreateEndingLabel(): LabelNode {
    val last = instructions.last
    if (last is LabelNode) return last
    val result = LabelNode()
    instructions.insert(last, result)
    return result
}

/*
 * Before 2.2.
 * We cannot extend a record if there is STORE instruction or a control-flow merge.
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
    liveness: List<VariableLivenessFrame>,
    nextSuspensionPointIndex: Int
): Boolean {
    val nextSuspensionPointLabel =
        suspensionPoints.drop(nextSuspensionPointIndex).find { it in InsnSequence(end, endLabel) } ?: endLabel

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
