/*
 * Copyright 2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.common.FastMethodAnalyzer
import org.jetbrains.kotlin.utils.forEachBit
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame
import org.jetbrains.org.objectweb.asm.tree.analysis.Interpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.Value
import java.util.*

// TODO rename everything. "Dead" is a bad name for this! It may be "uninitilized", "partially uninitialized",
//  "not fully initilized" etc. Or maybe just "valid"/"invalid"

// Predicts state of locals slots after resuming on `i-th` suspension point in case if no spills/restores
// were added. Used to find out what slots would require extra care (like default-initialization on restore point)
// if the following conditions are met:
// - the slot is "dead" on SP_i and is not selected for spill
// - the slot is "dead" on SP_j BUT is selected for spill there
// - there is a path SP_i -> SP_j with no STORE to that slot
internal fun performUninitializedAfterResumeVariablesAnalysis(
    suspensionPoints: List<SuspensionPoint>,
    methodNode: MethodNode,
    thisName: String,
): Array<out Frame<ResumeDependentValue>?> {
    val context = DeadAfterResumeVariablesContext(suspensionPoints)
    val analyzer = FastMethodAnalyzer(
        thisName, methodNode, EmptyInterpeter(), false
    ) { nLocals, nStack -> DeadAfterResumeVariablesFrame(context, nLocals) }
    // TODO maybe shrink frames before return, e.g. Map<Pair<SP,SP>, BitSet>
    return analyzer.analyze()
}

internal class EmptyInterpeter<V: Value> : Interpreter<V>(Opcodes.API_VERSION) {
    override fun newValue(type: Type?): V? = null
    override fun newOperation(insn: AbstractInsnNode?): V? = null
    override fun copyOperation(insn: AbstractInsnNode?, value: V?): V? = null
    override fun unaryOperation(insn: AbstractInsnNode?, value: V?): V? = null
    override fun binaryOperation(insn: AbstractInsnNode?, value1: V?, value2: V?): V? = null
    override fun ternaryOperation(insn: AbstractInsnNode?, value1: V?, value2: V?, value3: V?): V? = null
    override fun naryOperation(insn: AbstractInsnNode?, values: List<V?>?): V? = null
    override fun returnOperation(insn: AbstractInsnNode?, value: V?, expected: V?) {}
    override fun merge(value1: V?, value2: V?): V? = null
}

internal enum class DeadAfterResumeVariableState {
    // not analyzed yet, or no path from suspension point
    UNKNOWN, // TODO use null instead? I.e. `Boolean?` instead of this enum
    // after SP and STORE
    INITIALIZED,
    // after SP, before STORE. Final state, takes precedence on INITIALIZED on merge. Uninitialized (at least on some execution paths)
    UNINITIALIZED;

    fun merge(other: DeadAfterResumeVariableState): DeadAfterResumeVariableState =
        when {
            this == UNKNOWN -> other
            this == UNINITIALIZED || other == UNINITIALIZED -> UNINITIALIZED
            else -> this
        }
}

internal class ResumeDependentValue(suspensionPointsCount: Int) : Value {
    val states: Array<DeadAfterResumeVariableState> = Array(suspensionPointsCount) { DeadAfterResumeVariableState.UNKNOWN }

    // state is special value, size is ignored
    override fun getSize(): Int = 1

    fun setState(spIndex: Int, state: DeadAfterResumeVariableState) {
        states[spIndex] = state
    }
}

// immmutable shared part of the frames used during this anslysis
private class DeadAfterResumeVariablesContext(suspensionPoints: List<SuspensionPoint>) {
    init {
        check(!suspensionPoints.isEmpty())
        check(suspensionPoints.all { it.suspensionCallEnd.opcode == Opcodes.INVOKESTATIC })
    }

    val suspensionPointIndexByEnd: Map<AbstractInsnNode, Int> =
        suspensionPoints.withIndex().associate { (i, sp) -> sp.suspensionCallEnd to i }

    val suspensionPointsCount = suspensionPoints.size
}

private class DeadAfterResumeVariablesFrame(val context: DeadAfterResumeVariablesContext, val maxLocals: Int): Frame<ResumeDependentValue>(maxLocals, 0) {

    val isAfterSuspensionPoint = BitSet(context.suspensionPointsCount)

    override fun init(frame: Frame<out ResumeDependentValue?>): Frame<ResumeDependentValue?>? {
        val other = frame as? DeadAfterResumeVariablesFrame ?: return super.init(frame)
        isAfterSuspensionPoint.or(other.isAfterSuspensionPoint)
        return super.init(frame)
    }

    override fun merge(
        frame: Frame<out ResumeDependentValue?>,
        interpreter: Interpreter<ResumeDependentValue?>?,
    ): Boolean {
        val other = frame as? DeadAfterResumeVariablesFrame ?: return super.merge(frame, interpreter)
        check(maxLocals == other.maxLocals)

        var changed = isAfterSuspensionPoint == other.isAfterSuspensionPoint
        isAfterSuspensionPoint.or(other.isAfterSuspensionPoint)

        for (varIndex in 0 until this.maxLocals) {
            val thisLocal = getLocal(varIndex)
            val otherLocal = other.getLocal(varIndex)
            for (spIndex in 0 until context.suspensionPointsCount) {
                val newState = thisLocal.states[spIndex].merge(otherLocal.states[spIndex])
                changed = changed || thisLocal.states[spIndex] != newState
                thisLocal.states[spIndex] = newState
            }
        }
        return changed
    }

    override fun execute(insn: AbstractInsnNode, unused: Interpreter<ResumeDependentValue>?) {
        when (insn.opcode) {
            Opcodes.INVOKESTATIC -> {
                // could be suspension point end instruction
                context.suspensionPointIndexByEnd[insn]?.let {
                    isAfterSuspensionPoint.set(it)
                    setAllLocalsState(it, DeadAfterResumeVariableState.UNINITIALIZED)
                }
            }
            in Opcodes.ISTORE..Opcodes.ASTORE -> {
                val varInsn = insn as VarInsnNode
                isAfterSuspensionPoint.forEachBit { setLocalState(varInsn.`var`, it, DeadAfterResumeVariableState.INITIALIZED) }
            }
            Opcodes.IINC -> {
                val iincInsn = insn as IincInsnNode
                isAfterSuspensionPoint.forEachBit { setLocalState(iincInsn.`var`, it, DeadAfterResumeVariableState.INITIALIZED) }
            }
        }
    }

    private fun setAllLocalsState(spIndex: Int, state: DeadAfterResumeVariableState) {
        (0 until maxLocals).forEach { setLocalState(it, spIndex, state) }
    }

    private fun setLocalState(varIndex: Int, spIndex: Int, state: DeadAfterResumeVariableState) {
        getLocal(varIndex).setState(spIndex, state)
    }
}

