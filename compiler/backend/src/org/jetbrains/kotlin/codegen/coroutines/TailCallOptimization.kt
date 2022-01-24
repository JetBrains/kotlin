/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.inline.isInlineMarker
import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.ControlFlowGraph
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.fixStack.top
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicInterpreter
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue
import org.jetbrains.org.objectweb.asm.tree.analysis.Frame

internal class MethodNodeExaminer(
    containingClassInternalName: String,
    val methodNode: MethodNode,
    suspensionPoints: List<SuspensionPoint>,
    disableTailCallOptimizationForFunctionReturningUnit: Boolean
) {
    private val frames: Array<Frame<BasicValue>?> =
        MethodTransformer.analyze(containingClassInternalName, methodNode, TcoInterpreter(suspensionPoints))
    private val controlFlowGraph = ControlFlowGraph.build(methodNode)

    private val safeUnitInstances = mutableSetOf<AbstractInsnNode>()
    private val popsBeforeSafeUnitInstances = mutableSetOf<AbstractInsnNode>()
    private val areturnsAfterSafeUnitInstances = mutableSetOf<AbstractInsnNode>()
    private val meaningfulSuccessorsCache = hashMapOf<AbstractInsnNode, List<AbstractInsnNode>>()

    // CHECKCAST is considered safe if it is right before ARETURN and right after suspension point
    // In this case, we can add check for COROUTINE_SUSPENDED, the same as we did for functions, returning Unit.
    private val safeCheckcasts = mutableSetOf<AbstractInsnNode>()

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

        fun AbstractInsnNode.isPartOfCheckcastChainBeforeAreturn(): Boolean {
            for (succ in meaningfulSuccessors()) {
                when (succ.opcode) {
                    Opcodes.CHECKCAST ->
                        if (!succ.isPartOfCheckcastChainBeforeAreturn()) return false

                    Opcodes.ARETURN -> {
                        // do nothing
                    }
                    else -> return false
                }
            }
            return true
        }

        val checkcasts = methodNode.instructions.filter { it.opcode == Opcodes.CHECKCAST }
        for (checkcast in checkcasts) {
            if (!checkcast.isPartOfCheckcastChainBeforeAreturn()) continue
            if (frames[methodNode.instructions.indexOf(checkcast)]?.top() !is FromSuspensionPointValue) continue
            safeCheckcasts += checkcast
        }
    }

    // GETSTATIC kotlin/Unit.INSTANCE is considered safe iff
    // it is part of POP, PUSH Unit, ARETURN sequence.
    private fun AbstractInsnNode.isSafeUnitInstance(): Boolean = this in safeUnitInstances

    private fun AbstractInsnNode.isPopBeforeSafeUnitInstance(): Boolean = this in popsBeforeSafeUnitInstances
    private fun AbstractInsnNode.isAreturnAfterSafeUnitInstance(): Boolean = this in areturnsAfterSafeUnitInstances

    private fun AbstractInsnNode.meaningfulSuccessors(): List<AbstractInsnNode> = meaningfulSuccessorsCache.getOrPut(this) {
        fun AbstractInsnNode.isMeaningful() = isMeaningful && opcode != Opcodes.NOP && opcode != Opcodes.GOTO && this !is LineNumberNode

        val visited = mutableSetOf<AbstractInsnNode>()
        fun dfs(insn: AbstractInsnNode) {
            if (insn in visited) return
            visited += insn
            if (!insn.isMeaningful()) {
                for (succIndex in controlFlowGraph.getSuccessorsIndices(insn)) {
                    dfs(methodNode.instructions[succIndex])
                }
            }
        }
        for (succIndex in controlFlowGraph.getSuccessorsIndices(this)) {
            dfs(methodNode.instructions[succIndex])
        }
        visited.filter { it.isMeaningful() }
    }

    fun replacePopsBeforeSafeUnitInstancesWithCoroutineSuspendedChecks() {
        val isReferenceMap =
            popsBeforeSafeUnitInstances.associateWith { (frames[methodNode.instructions.indexOf(it)]?.top()?.isReference == true) }

        for (pop in popsBeforeSafeUnitInstances) {
            if (isReferenceMap[pop] == true) {
                val label = Label()
                methodNode.instructions.insertBefore(pop, withInstructionAdapter {
                    dup()
                    loadCoroutineSuspendedMarker()
                    ifacmpne(label)
                    areturn(AsmTypes.OBJECT_TYPE)
                    mark(label)
                })
            }
        }
    }

    fun addCoroutineSuspendedChecksBeforeSafeCheckcasts() {
        for (checkcast in safeCheckcasts) {
            val label = Label()
            methodNode.instructions.insertBefore(checkcast, withInstructionAdapter {
                dup()
                loadCoroutineSuspendedMarker()
                ifacmpne(label)
                areturn(AsmTypes.OBJECT_TYPE)
                mark(label)
            })
        }
    }

    fun allSuspensionPointsAreTailCalls(suspensionPoints: List<SuspensionPoint>): Boolean {
        val safelyReachableReturns = findSafelyReachableReturns()

        val instructions = methodNode.instructions
        return suspensionPoints.all { suspensionPoint ->
            val beginIndex = instructions.indexOf(suspensionPoint.suspensionCallBegin)
            val endIndex = instructions.indexOf(suspensionPoint.suspensionCallEnd)

            val insideTryBlock = methodNode.tryCatchBlocks.any { block ->
                val tryBlockStartIndex = instructions.indexOf(block.start)
                val tryBlockEndIndex = instructions.indexOf(block.end)

                beginIndex in tryBlockStartIndex until tryBlockEndIndex
            }
            if (insideTryBlock) return@all false

            safelyReachableReturns[endIndex + 1]?.all { returnIndex ->
                frames[returnIndex]?.top().sure {
                    "There must be some value on stack to return"
                } is FromSuspensionPointValue
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
        val insns = methodNode.instructions.toArray()
        val reachableReturnsIndices = Array(insns.size) init@{ index ->
            val insn = insns[index]

            if (insn.opcode == Opcodes.ARETURN && !insn.isAreturnAfterSafeUnitInstance()) {
                return@init setOf(index)
            }

            // Since POP, PUSH Unit, ARETURN behaves like normal return in terms of tail-call optimization, set return index to POP
            if (insn.isPopBeforeSafeUnitInstance()) {
                return@init setOf(index)
            }

            if (!insn.isMeaningful || insn.opcode in SAFE_OPCODES || insn.isInvisibleInDebugVarInsn(methodNode) || isInlineMarker(insn)
                || insn.isSafeUnitInstance() || insn.isAreturnAfterSafeUnitInstance()
            ) {
                setOf()
            } else null
        }

        var changed: Boolean
        do {
            changed = false
            for (index in insns.indices.reversed()) {
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

private fun AbstractInsnNode?.isInvisibleInDebugVarInsn(methodNode: MethodNode): Boolean {
    val insns = methodNode.instructions
    val index = insns.indexOf(this)
    return (this is VarInsnNode && methodNode.localVariables.none {
        it.index == `var` && index in it.start.let(insns::indexOf)..it.end.let(insns::indexOf)
    })
}

private val SAFE_OPCODES =
    ((Opcodes.DUP..Opcodes.DUP2_X2) + Opcodes.NOP + Opcodes.POP + Opcodes.POP2 + (Opcodes.IFEQ..Opcodes.GOTO)).toSet() + Opcodes.CHECKCAST

private object FromSuspensionPointValue : BasicValue(AsmTypes.OBJECT_TYPE) {
    override fun equals(other: Any?): Boolean = other is FromSuspensionPointValue
}

private fun BasicValue?.toFromSuspensionPoint(): BasicValue? = if (this?.type?.sort == Type.OBJECT) FromSuspensionPointValue else this

private class TcoInterpreter(private val suspensionPoints: List<SuspensionPoint>) : BasicInterpreter(Opcodes.API_VERSION) {
    override fun copyOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        return super.copyOperation(insn, value).convert(insn)
    }

    private fun BasicValue?.convert(insn: AbstractInsnNode): BasicValue? = if (insn in suspensionPoints) toFromSuspensionPoint() else this

    override fun naryOperation(insn: AbstractInsnNode, values: MutableList<out BasicValue?>?): BasicValue? {
        return super.naryOperation(insn, values).convert(insn)
    }

    override fun ternaryOperation(insn: AbstractInsnNode, value1: BasicValue?, value2: BasicValue?, value3: BasicValue?): BasicValue? {
        return super.ternaryOperation(insn, value1, value2, value3).convert(insn)
    }

    override fun merge(value1: BasicValue?, value2: BasicValue?): BasicValue {
        return if (value1 is FromSuspensionPointValue || value2 is FromSuspensionPointValue) FromSuspensionPointValue
        else super.merge(value1, value2)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: BasicValue?): BasicValue? {
        // Assume, that CHECKCAST Object does not break tail-call optimization
        if (value is FromSuspensionPointValue && insn.opcode == Opcodes.CHECKCAST) {
            return value
        }
        return super.unaryOperation(insn, value).convert(insn)
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: BasicValue?, value2: BasicValue?): BasicValue? {
        return super.binaryOperation(insn, value1, value2).convert(insn)
    }

    override fun newOperation(insn: AbstractInsnNode): BasicValue? {
        return super.newOperation(insn).convert(insn)
    }
}
