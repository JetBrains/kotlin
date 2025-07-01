/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.google.common.collect.LinkedListMultimap
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.*
import kotlin.math.max

abstract class CoveringTryCatchNodeProcessor(parameterSize: Int) {
    val tryBlocksMetaInfo: IntervalMetaInfo<TryCatchBlockNodeInfo> = IntervalMetaInfo(this)
    val localVarsMetaInfo: IntervalMetaInfo<LocalVarNodeWrapper> = IntervalMetaInfo(this)

    var nextFreeLocalIndex: Int = parameterSize
        private set

    fun getStartNodes(label: LabelNode): List<TryCatchBlockNodeInfo> {
        return tryBlocksMetaInfo.intervalStarts.get(label)
    }

    fun getEndNodes(label: LabelNode): List<TryCatchBlockNodeInfo> {
        return tryBlocksMetaInfo.intervalEnds.get(label)
    }

    open fun processInstruction(curInstr: AbstractInsnNode, directOrder: Boolean) {
        if (curInstr is VarInsnNode || curInstr is IincInsnNode) {
            val argSize = getLoadStoreArgSize(curInstr.opcode)
            val varIndex = if (curInstr is VarInsnNode) curInstr.`var` else (curInstr as IincInsnNode).`var`
            nextFreeLocalIndex = max(nextFreeLocalIndex, varIndex + argSize)
        }

        if (curInstr is LabelNode) {
            tryBlocksMetaInfo.processCurrent(curInstr, directOrder)
            localVarsMetaInfo.processCurrent(curInstr, directOrder)
        }
    }

    abstract fun instructionIndex(inst: AbstractInsnNode): Int

    fun sortTryCatchBlocks(intervals: List<TryCatchBlockNodeInfo>): List<TryCatchBlockNodeInfo> {
        val comp = Comparator { t1: TryCatchBlockNodeInfo, t2: TryCatchBlockNodeInfo ->
            var result = instructionIndex(t1.handler) - instructionIndex(t2.handler)
            if (result == 0) {
                result = instructionIndex(t1.startLabel) - instructionIndex(t2.startLabel)
                if (result == 0) {
                    assert(false) { "Error: support multicatch finallies: ${t1.handler}, ${t2.handler}" }
                    result = instructionIndex(t1.endLabel) - instructionIndex(t2.endLabel)
                }
            }
            result
        }

        Collections.sort(intervals, comp)
        return intervals
    }

    fun substituteTryBlockNodes(node: MethodNode) {
        node.tryCatchBlocks.clear()
        sortTryCatchBlocks(tryBlocksMetaInfo.allIntervals)
        for (info in tryBlocksMetaInfo.getMeaningfulIntervals()) {
            node.tryCatchBlocks.add(info.node)
        }
    }

    fun substituteLocalVarTable(node: MethodNode) {
        node.localVariables.clear()
        for (info in localVarsMetaInfo.getMeaningfulIntervals()) {
            node.localVariables.add(info.node)
        }
    }
}

class IntervalMetaInfo<T : SplittableInterval<T>>(private val processor: CoveringTryCatchNodeProcessor) {
    val intervalStarts = LinkedListMultimap.create<LabelNode, T>()
    val intervalEnds = LinkedListMultimap.create<LabelNode, T>()
    val allIntervals: ArrayList<T> = arrayListOf()
    val currentIntervals: MutableSet<T> = linkedSetOf()

    fun intersection(instructions: InsnList, interval: T, other: Interval): T? {
        val startIndex = instructions.indexOf(interval.startLabel)
        val endIndex = instructions.indexOf(interval.endLabel)
        val otherStartIndex = instructions.indexOf(other.startLabel)
        val otherEndIndex = instructions.indexOf(other.endLabel)

        // Two intervals [a,b] and [c,d] intersect if max(a,c) < min(b,d), and the intersection is [max(a,c), min(b,d))
        val newStartIndex = maxOf(startIndex, otherStartIndex)
        val newEndIndex = minOf(endIndex, otherEndIndex)

        if (newStartIndex >= newEndIndex) {
            // no intersection
            return null
        }

        val newStartLabel = if (startIndex == newStartIndex) interval.startLabel else other.startLabel
        val newEndLabel = if (endIndex == newEndIndex) interval.endLabel else other.endLabel
        return interval.copyWithNewBounds(SimpleInterval(newStartLabel, newEndLabel))
    }

    fun copyIntervalsForRange(instructions: InsnList, startLabel: LabelNode, endLabel: LabelNode): List<T> {
        val finallyInterval = SimpleInterval(startLabel, endLabel)
        return allIntervals.mapNotNull { intersection(instructions, it, finallyInterval) }
    }

    fun addNewInterval(newInfo: T) {
        newInfo.verify(processor)
        intervalStarts.put(newInfo.startLabel, newInfo)
        intervalEnds.put(newInfo.endLabel, newInfo)
        allIntervals.add(newInfo)
    }

    private fun remapStartLabel(oldStart: LabelNode, remapped: T) {
        remapped.verify(processor)
        intervalStarts.remove(oldStart, remapped)
        intervalStarts.put(remapped.startLabel, remapped)
    }

    private fun remapEndLabel(oldEnd: LabelNode, remapped: T) {
        remapped.verify(processor)
        intervalEnds.remove(oldEnd, remapped)
        intervalEnds.put(remapped.endLabel, remapped)
    }

    fun splitCurrentIntervals(by: Interval, keepStart: Boolean): List<SplitPair<T>> {
        return currentIntervals.map { split(it, by, keepStart) }
    }

    fun splitAndRemoveCurrentIntervals(by: Interval, keepStart: Boolean) {
        currentIntervals.toList().forEach { splitAndRemoveIntervalFromCurrents(it, by, keepStart) }
    }

    fun processCurrent(curIns: LabelNode, directOrder: Boolean) {
        getInterval(curIns, directOrder).forEach {
            val added = currentIntervals.add(it)
            assert(added) { "Wrong interval structure: $curIns, $it" }
        }

        getInterval(curIns, !directOrder).forEach {
            val removed = currentIntervals.remove(it)
            assert(removed) { "Wrong interval structure: $curIns, $it" }
        }
    }

    fun split(interval: T, by: Interval, keepStart: Boolean): SplitPair<T> {
        val split = interval.split(by, keepStart)
        if (!keepStart) {
            remapStartLabel(split.newPart.startLabel, split.patchedPart)
        } else {
            remapEndLabel(split.newPart.endLabel, split.patchedPart)
        }
        addNewInterval(split.newPart)
        return split
    }

    fun splitAndRemoveIntervalFromCurrents(interval: T, by: Interval, keepStart: Boolean): SplitPair<T> {
        val splitPair = split(interval, by, keepStart)
        val removed = currentIntervals.remove(splitPair.patchedPart)
        assert(removed) { "Wrong interval structure: $splitPair" }
        return splitPair
    }

    private fun getInterval(curIns: LabelNode, isOpen: Boolean) =
        if (isOpen) intervalStarts.get(curIns) else intervalEnds.get(curIns)
}

fun TryCatchBlockNode.isMeaningless() = SimpleInterval(start, end).isMeaningless()

fun Interval.isMeaningless(): Boolean {
    val start = this.startLabel
    var end: AbstractInsnNode = this.endLabel
    while (end != start && !end.isMeaningful) {
        end = end.previous
    }
    return start == end
}

fun <T : SplittableInterval<T>> IntervalMetaInfo<T>.getMeaningfulIntervals(): List<T> {
    return allIntervals.filterNot { it.isMeaningless() }
}

class DefaultProcessor(val node: MethodNode, parameterSize: Int) : CoveringTryCatchNodeProcessor(parameterSize) {
    init {
        node.tryCatchBlocks.forEach {
            tryBlocksMetaInfo.addNewInterval(TryCatchBlockNodeInfo(it, false))
        }
        node.localVariables.forEach {
            localVarsMetaInfo.addNewInterval(LocalVarNodeWrapper(it))
        }
    }

    override fun instructionIndex(inst: AbstractInsnNode): Int = node.instructions.indexOf(inst)
}

class LocalVarNodeWrapper(val node: LocalVariableNode) : Interval, SplittableInterval<LocalVarNodeWrapper> {
    override val startLabel: LabelNode
        get() = node.start
    override val endLabel: LabelNode
        get() = node.end

    override fun split(splitBy: Interval, keepStart: Boolean): SplitPair<LocalVarNodeWrapper> {
        val newPartInterval = if (keepStart) {
            val oldEnd = endLabel
            node.end = splitBy.startLabel
            SimpleInterval(splitBy.endLabel, oldEnd)
        } else {
            val oldStart = startLabel
            node.start = splitBy.endLabel
            SimpleInterval(oldStart, splitBy.startLabel)
        }

        return SplitPair(this, copyWithNewBounds(newPartInterval))
    }

    override fun copyWithNewBounds(newBounds: Interval): LocalVarNodeWrapper =
        LocalVarNodeWrapper(LocalVariableNode(node.name, node.desc, node.signature, newBounds.startLabel, newBounds.endLabel, node.index))

    fun remapLabel(oldLabel: LabelNode, newLabel: LabelNode) {
        if (node.start == oldLabel) {
            node.start = newLabel
        }
        if (node.end == oldLabel) {
            node.end = newLabel
        }
    }
}
