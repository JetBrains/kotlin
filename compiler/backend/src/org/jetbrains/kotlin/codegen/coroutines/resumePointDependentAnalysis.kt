/*
 * Copyright 2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.inline.isAfterSuspendMarker
import org.jetbrains.kotlin.codegen.optimization.common.ControlFlowGraph
import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.utils.copy
import org.jetbrains.kotlin.utils.forEachBit
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.IincInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value
import java.util.BitSet
import kotlin.time.measureTimedValue

@Suppress("unused")
private const val COMPARE_ALGORITHMS_PERFORMANCE = false

// Returns an array of lists (one per suspension point) of local slots that need to be re-initialized on resume of that SP.
// Such re-initialization on restore points is required if the following conditions are met:
// - the slot is "dead" on SP_i and is not selected for spill
// - the slot is "dead" on SP_j BUT is selected for spill there
// - there is a path SP_i -> SP_j with no STORE to that slot
internal fun calculateVariablesToReinitializeBySuspensionPoint(
    suspensionPoints: List<SuspensionPoint>,
    methodNode: MethodNode,
    containingClassName: String,
    variablesToSpillBySuspensionPointIndex: List<List<SpillableVariable>>,
): Array<MutableList<SpillableVariable>> {

    fun createAlgorithm(useDFAVersion: Boolean) = if (useDFAVersion) {
        ReinitializationAnalysisUsingDFA(suspensionPoints, methodNode, containingClassName, variablesToSpillBySuspensionPointIndex)
    } else {
        ReinitializationAnalysisUsingDFS(suspensionPoints, methodNode, containingClassName, variablesToSpillBySuspensionPointIndex)
    }

    if (COMPARE_ALGORITHMS_PERFORMANCE) {
        createAlgorithm(false).calculateAndPrintPerformanceStats()
        return createAlgorithm(true).calculateAndPrintPerformanceStats()
    } else {
        // Based on the performance measurements on `kt83372.kt`, the DFS version is almost always faster, except for the
        // case of a large number of variables and the complicated CFG. The DFA version is left mainly for the reference; but
        // maybe there will be other cases in the future where it will be much faster than the DFS one
        return createAlgorithm(false).calculate()
    }
}

private abstract class ReinitializationAnalysis(
    val suspensionPoints: List<SuspensionPoint>,
    val methodNode: MethodNode,
    val containingClassName: String,
    val variablesToSpillBySuspensionPointIndex: List<List<SpillableVariable>>
) {
    abstract fun calculate(): Array<MutableList<SpillableVariable>>

    fun calculateAndPrintPerformanceStats(): Array<MutableList<SpillableVariable>> {
        val (result, time) = measureTimedValue { calculate() }
        println(
            "${this::class.simpleName}.calculate() for $containingClassName::${methodNode.name} " +
                    "took ${time.inWholeMilliseconds} ms; nSP=${suspensionPoints.size}, nLocals=${methodNode.maxLocals}, " +
                    "nInstructions=${methodNode.instructions.size()}"
        )
        return result
    }
}

// Calculates variables that need re-initialization by performing multiple Reverse-DFS (for each suspension point and
// each variable spilled on that SP). Each DFS finds paths from all previous SPs to the current one where the variable
// is not initialized.
private class ReinitializationAnalysisUsingDFS(
    suspensionPoints: List<SuspensionPoint>,
    methodNode: MethodNode,
    containingClassName: String,
    variablesToSpillBySuspensionPointIndex: List<List<SpillableVariable>>
) : ReinitializationAnalysis(suspensionPoints, methodNode, containingClassName, variablesToSpillBySuspensionPointIndex) {

    override fun calculate(): Array<MutableList<SpillableVariable>> {
        val variablesForReinitializationBySuspensionPointIndex =
            Array<MutableList<SpillableVariable>>(suspensionPoints.size) { mutableListOf() }
        val cfg = ControlFlowGraph.build(methodNode)
        val spEnds: List<Int> = suspensionPoints.map { methodNode.instructions.indexOf(it.suspensionCallEnd) }
        val spIndexBySpEnd = spEnds.withIndex().associateBy({ it.value }, { it.index })
        for ((spIndex, suspensionPoint) in suspensionPoints.withIndex()) {
            for (variable in variablesToSpillBySuspensionPointIndex[spIndex]) {
                val slot = variable.slot
                val start: Int = methodNode.instructions.indexOf(suspensionPoint.suspensionCallBegin)
                val curEnd = methodNode.instructions.indexOf(suspensionPoint.suspensionCallEnd)
                // reverse-dfs from start until ends, with a path stopped by any end or init of the slot
                val spEndsWithUninitializedSlotPath: List<Int> = doDFS(start, cfg, spEnds, curEnd, slot)

                spEndsWithUninitializedSlotPath.filter { it != curEnd }.forEach {
                    spIndexBySpEnd[it]!!.let { otherSpIndex ->
                        if (variablesToSpillBySuspensionPointIndex[otherSpIndex].all { it.slot != variable.slot })
                            variablesForReinitializationBySuspensionPointIndex[otherSpIndex].add(variable)
                    }
                }
            }
        }
        return variablesForReinitializationBySuspensionPointIndex
    }

    private fun doDFS(
        startInsnIndex: Int,
        cfg: ControlFlowGraph,
        spEnds: List<Int>,
        currentSPEnd: Int,
        slot: Int,
    ): List<Int> {
        val foundSpEndsWithUninitializedPathFrom = mutableListOf<Int>()
        fun handleBeforeChildren(insnIndex: Int): Boolean {
            if (spEnds.contains(insnIndex)) {
                if (insnIndex != currentSPEnd) {
                    // found a way from another SP to the current SP where the local is not initialized
                    foundSpEndsWithUninitializedPathFrom.add(insnIndex)
                }
                // no need to go further, as the variable will be either spilled or re-initialized on this SP
                return false
            }
            val insn = methodNode.instructions[insnIndex]
            val storedSlotOrNull = when (insn.opcode) {
                in Opcodes.ISTORE..Opcodes.ASTORE -> (insn as VarInsnNode).`var`
                Opcodes.IINC -> (insn as IincInsnNode).`var`
                else -> null
            }
            if (storedSlotOrNull == slot) {
                // no need to go further, as the variable is initialized on this path
                return false
            }
            return true
        }

        val stack = ArrayDeque<Int>()
        val visited = BitSet(methodNode.instructions.size()).also { it.set(startInsnIndex) }
        stack.add(startInsnIndex)

        while (stack.isNotEmpty()) {
            val insnIndex = stack.removeLast()
            if (!handleBeforeChildren(insnIndex)) continue

            cfg.getPredecessorsIndices(insnIndex).forEach { pred ->
                if (!visited[pred]) {
                    visited.set(pred)
                    stack.add(pred)
                }
            }
        }

        return foundSpEndsWithUninitializedPathFrom
    }
}

// Calculates variables that need re-initialization by performing a single Data-Flow Analysis that predicts states
// of local slots after resuming on `i-th` suspension point in case if no spills/restores were added.
private class ReinitializationAnalysisUsingDFA(
    suspensionPoints: List<SuspensionPoint>,
    methodNode: MethodNode,
    containingClassName: String,
    variablesToSpillBySuspensionPointIndex: List<List<SpillableVariable>>
) : ReinitializationAnalysis(suspensionPoints, methodNode, containingClassName, variablesToSpillBySuspensionPointIndex) {

    override fun calculate(): Array<MutableList<SpillableVariable>> {
        val context = SuspensionPointsContext(suspensionPoints)
        // NOTE: exception edges shall not be "pruned" as path from SP to exception handlers are important for this analysis
        val analyzer = FastMethodAnalyzer(
            containingClassName, methodNode, ResumeDependentInterpeter(context), false
        ) { nLocals, _ -> ResumeDependentFrame(context, nLocals) }
        val afterResumeFrames = analyzer.analyze()
        val variablesForReinitializationBySuspensionPointIndex =
            Array<MutableList<SpillableVariable>>(suspensionPoints.size) { mutableListOf() }
        for ((spIndex, suspensionPoint) in suspensionPoints.withIndex()) {
            val resumeDependentFrame = afterResumeFrames[methodNode.instructions.indexOf(suspensionPoint.suspensionCallEnd)]
                ?: error(
                    "Missing 'after resume' analysis data for ${suspensionPoint.suspensionCallEnd} " +
                            "at ${containingClassName}::${methodNode.name}"
                )
            for (variable in variablesToSpillBySuspensionPointIndex[spIndex]) {
                val resumeDependentValue = resumeDependentFrame.getLocal(variable.slot)
                    ?: error("Missing 'after resume' analysis data for slot ${variable.slot}")
                resumeDependentValue.states.withIndex().filter { it.value.isUnitialized() && it.index != spIndex }
                    .forEach { indexAndValue ->
                        val otherSpIndex = indexAndValue.index
                        // the analysis deduced that there is a path between suspension points (otherSpIndex -> spIndex) with no
                        // STOREs to the variable's slot
                        if (variablesToSpillBySuspensionPointIndex[otherSpIndex].all { it.slot != variable.slot }) {
                            // ... and the variable is not spilled on preceding (other) SP, so we need to additionally initialize it
                            // with some value (e.g. default one) on unspill block
                            val variablesForReinitialization = variablesForReinitializationBySuspensionPointIndex[otherSpIndex]
                            if (variablesForReinitialization.none { it.slot == variable.slot })
                                variablesForReinitialization.add(variable)
                        }
                    }
            }
        }
        return variablesForReinitializationBySuspensionPointIndex
    }

    // States of a single variable on some instruction for each of "resumed on SP_i" cases
    private class ResumeDependentValue(val states: Array<VariableState>) : Value {

        constructor(suspensionPointsCount: Int) : this(Array(suspensionPointsCount) { VariableState.UNKNOWN })

        fun withState(spIndex: Int, state: VariableState) : ResumeDependentValue =
            if (states[spIndex] == state) this else ResumeDependentValue(states.copyOf().apply { set(spIndex, state) })

        // size is ignored for such special Value
        override fun getSize(): Int = 1

        override fun toString(): String = states.joinToString(separator = "", prefix="[", postfix = "]") { it.toString() }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ResumeDependentValue) return false

            return states.contentEquals(other.states)
        }

        override fun hashCode(): Int {
            return states.contentHashCode()
        }

        // Variable state on some instruction in case of resume on a given suspension point
        enum class VariableState {
            // not analyzed yet, or no path from the suspension point
            UNKNOWN,
            // initialized by some STORE on each path from the suspension point to the instruction
            INITIALIZED,
            // not initialized on at least one path from the suspension point to the instruction
            UNINITIALIZED;

            fun isUnitialized() = this == UNINITIALIZED

            fun merge(other: VariableState): VariableState =
                when {
                    this == UNKNOWN -> other
                    this == UNINITIALIZED || other == UNINITIALIZED -> UNINITIALIZED
                    else -> this
                }

            override fun toString(): String =
                when (this) {
                    UNKNOWN -> "?"
                    UNINITIALIZED -> "!"
                    INITIALIZED -> "+"
                }
        }
    }

    // immmutable shared part of the frames used during this analysis
    private class SuspensionPointsContext(suspensionPoints: List<SuspensionPoint>) {
        init {
            check(suspensionPoints.isNotEmpty())
            check(suspensionPoints.all { isAfterSuspendMarker(it.suspensionCallEnd) })
        }

        val suspensionPointIndexByEnd: Map<AbstractInsnNode, Int> =
            suspensionPoints.withIndex().associate { (i, sp) -> sp.suspensionCallEnd to i }

        val suspensionPointsCount = suspensionPoints.size
    }

    private class ResumeDependentFrame(val context: SuspensionPointsContext, val maxLocals: Int): Frame<ResumeDependentValue>(maxLocals, 0) {
        val isAfterSuspensionPoint = BitSet(context.suspensionPointsCount)

        init {
            for (varIndex in 0 until maxLocals) {
                setLocal(varIndex, ResumeDependentValue(context.suspensionPointsCount))
            }
        }

        override fun toString(): String {
            return "After ${isAfterSuspensionPoint}, " + super.toString()
        }

        override fun init(frame: Frame<out ResumeDependentValue?>): Frame<ResumeDependentValue?>? {
            val other = frame as? ResumeDependentFrame ?: return super.init(frame)
            isAfterSuspensionPoint.clear()
            isAfterSuspensionPoint.or(other.isAfterSuspensionPoint)
            return super.init(frame)
        }

        override fun merge(
            frame: Frame<out ResumeDependentValue?>,
            interpreter: Interpreter<ResumeDependentValue?>?,
        ): Boolean {
            val other = frame as? ResumeDependentFrame ?: return super.merge(frame, interpreter)
            check(maxLocals == other.maxLocals)

            val changed = if (isAfterSuspensionPoint != other.isAfterSuspensionPoint) {
                val prev = isAfterSuspensionPoint.copy()
                isAfterSuspensionPoint.or(other.isAfterSuspensionPoint)
                isAfterSuspensionPoint != prev
            } else false

            return super.merge(frame, interpreter) || changed
        }

        override fun execute(insn: AbstractInsnNode, unused: Interpreter<ResumeDependentValue>?) {
            if (isAfterSuspendMarker(insn)) {
                context.suspensionPointIndexByEnd[insn]?.let {
                    isAfterSuspensionPoint.set(it)
                    setAllLocalsState(it, ResumeDependentValue.VariableState.UNINITIALIZED)
                }
            }
            when (insn.opcode) {
                in Opcodes.ISTORE..Opcodes.ASTORE -> {
                    val varInsn = insn as VarInsnNode
                    isAfterSuspensionPoint.forEachBit { setLocalState(varInsn.`var`, it, ResumeDependentValue.VariableState.INITIALIZED) }
                }
                Opcodes.IINC -> {
                    val iincInsn = insn as IincInsnNode
                    isAfterSuspensionPoint.forEachBit { setLocalState(iincInsn.`var`, it, ResumeDependentValue.VariableState.INITIALIZED) }
                }
            }
        }

        private fun setAllLocalsState(spIndex: Int, state: ResumeDependentValue.VariableState) {
            (0 until maxLocals).forEach { setLocalState(it, spIndex, state) }
        }

        private fun setLocalState(varIndex: Int, spIndex: Int, state: ResumeDependentValue.VariableState) {
            setLocal(varIndex, getLocal(varIndex).withState(spIndex, state))
        }
    }

    private class ResumeDependentInterpeter(val context: SuspensionPointsContext) : Interpreter<ResumeDependentValue>(Opcodes.API_VERSION) {
        // The only used methods in this interpreter are `newValue()` called during the parameters initialization, and `merge()`
        // No actual instructions interpretation is implemented or used, as it is frame-dependent, so is performed
        // solely in ResumeDependentFrame.execute() method
        override fun newValue(type: Type?): ResumeDependentValue = ResumeDependentValue(context.suspensionPointsCount)

        override fun merge(value1: ResumeDependentValue, value2: ResumeDependentValue): ResumeDependentValue {
            if (value1 == value2) return value1
            val mergedStates = (0 until context.suspensionPointsCount)
                .map { spIndex -> value1.states[spIndex].merge(value2.states[spIndex]) }
                .toTypedArray()
            return ResumeDependentValue(mergedStates)
        }

        override fun newOperation(insn: AbstractInsnNode?): ResumeDependentValue? = null
        override fun copyOperation(insn: AbstractInsnNode?, value: ResumeDependentValue?): ResumeDependentValue? = null
        override fun unaryOperation(insn: AbstractInsnNode?, value: ResumeDependentValue?): ResumeDependentValue? = null

        override fun binaryOperation(
            insn: AbstractInsnNode?,
            value1: ResumeDependentValue?,
            value2: ResumeDependentValue?,
        ): ResumeDependentValue? = null

        override fun ternaryOperation(
            insn: AbstractInsnNode?,
            value1: ResumeDependentValue?,
            value2: ResumeDependentValue?,
            value3: ResumeDependentValue?,
        ): ResumeDependentValue? = null

        override fun naryOperation(
            insn: AbstractInsnNode?,
            values: List<ResumeDependentValue?>?,
        ): ResumeDependentValue? = null

        override fun returnOperation(
            insn: AbstractInsnNode?,
            value: ResumeDependentValue?,
            expected: ResumeDependentValue?,
        ) {
            // No implementation needed
        }
    }
}

