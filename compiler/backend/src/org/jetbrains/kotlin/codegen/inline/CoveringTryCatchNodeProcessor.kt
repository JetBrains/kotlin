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
import java.util.ArrayList
import com.intellij.util.containers.Stack
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.Comparator
import java.util.Collections

public abstract class CoveringTryCatchNodeProcessor<T : IntervalWithHandler>  where T : SplittableInterval<T> {

    public val tryBlocksMetaInfo: IntervalMetaInfo<T> = IntervalMetaInfo();

    public val localVarsMetaInfo: IntervalMetaInfo<LocalVarNodeWrapper> = IntervalMetaInfo();

    public val coveringFromInnermost: List<T>
        get() = tryBlocksMetaInfo.currentIntervals.reverse()

    public fun getStartNodes(label: LabelNode): List<T> {
        return tryBlocksMetaInfo.intervalStarts.get(label)
    }

    public fun getEndNodes(label: LabelNode): List<T> {
        return tryBlocksMetaInfo.intervalEnds.get(label)
    }

    //Keep information about try blocks that cover current instruction -
    // pushing and popping it to stack entering and exiting tryCatchBlock start and end labels
    public open fun updateCoveringTryBlocks(curIns: AbstractInsnNode, directOrder: Boolean) {
        if (curIns !is LabelNode) return

        for (startNode in tryBlocksMetaInfo.infoToClose(curIns, directOrder)) {
            val pop = tryBlocksMetaInfo.currentIntervals.pop()
            //Temporary disabled cause during patched structure of exceptions changed
//            if (startNode != pop) {
//                throw RuntimeException("Wrong try-catch structure " + startNode + " " + pop + " " + infosToClose.size())
//            };
        }

        //Reversing list order cause we should pop external block before internal one
        // (originally internal blocks goes before external one, such invariant preserved via sortTryCatchBlocks method)
        for (info in tryBlocksMetaInfo.infoToOpen(curIns, directOrder).reverse()) {
            tryBlocksMetaInfo.currentIntervals.add(info)
        }
    }

    public open fun updateCoveringLocalVars(curIns: AbstractInsnNode, directOrder: Boolean) {
        if (curIns !is LabelNode) return

        localVarsMetaInfo.infoToClose(curIns, directOrder).forEach {
            localVarsMetaInfo.currentIntervals.pop()
        }

        localVarsMetaInfo.infoToOpen(curIns, directOrder).forEach {
            localVarsMetaInfo.currentIntervals.add(it)
        }
    }

    public abstract fun instructionIndex(inst: AbstractInsnNode): Int

    public fun sortTryCatchBlocks() {
        val comp = Comparator { t1: T, t2: T ->
            var result = instructionIndex(t1.handler) - instructionIndex(t2.handler)
            if (result == 0) {
                result = instructionIndex((t1 as Interval).startLabel) - instructionIndex((t2 as Interval).startLabel)
                if (result == 0) {
                    assert(false, "Error: support multicatch finallies!")
                    result = instructionIndex((t1 as Interval).endLabel) - instructionIndex((t2 as Interval).endLabel)
                }
            }
            result
        }

        Collections.sort<T>(tryBlocksMetaInfo.allIntervals, comp)
    }
}

class IntervalMetaInfo<T : Interval> {

    val intervalStarts = LinkedListMultimap.create<LabelNode, T>()

    val intervalEnds = LinkedListMultimap.create<LabelNode, T>()

    val allIntervals: ArrayList<T> = arrayListOf()

    val currentIntervals: Stack<T> = Stack()

    fun addNewInterval(newInfo: T) {
        intervalStarts.put(newInfo.startLabel, newInfo)
        intervalEnds.put(newInfo.endLabel, newInfo)
        allIntervals.add(newInfo)
    }

    fun remapStartLabel(oldStart: LabelNode, remapped: T) {
        intervalStarts.remove(oldStart, remapped)
        intervalStarts.put(remapped.startLabel, remapped)
    }

    fun remapEndLabel(oldEnd: LabelNode, remapped: T) {
        intervalEnds.remove(oldEnd, remapped)
        intervalEnds.put(remapped.endLabel, remapped)
    }

    fun split(interval: SplittableInterval<T>, by : Interval, keepStart: Boolean): SplittedPair<T> {
        val splittedPair = interval.split(by, keepStart)
        if (!keepStart) {
            remapStartLabel((splittedPair.newPart as Interval).startLabel, splittedPair.patchedPart)
        } else {
            remapEndLabel((splittedPair.newPart as Interval).endLabel, splittedPair.patchedPart)
        }
        addNewInterval(splittedPair.newPart)
        return splittedPair
    }

    public fun getNonEmptyNodes(): List<T> {
        return allIntervals.filterNot { isEmptyInterval(it) }
    }

    fun infoToClose(curIns: LabelNode, directOrder: Boolean) = if (!directOrder) intervalStarts.get(curIns) else intervalEnds.get(curIns)

    fun infoToOpen(curIns: LabelNode, directOrder: Boolean) = if (directOrder) intervalStarts.get(curIns) else intervalEnds.get(curIns)

    private fun isEmptyInterval(node: T): Boolean {
        val start = node.startLabel
        var end: AbstractInsnNode = node.endLabel
        while (end != start && end is LabelNode) {
            end = end.getPrevious()
        }
        return start == end;
    }
}

public class DefaultProcessor(val node: MethodNode) : CoveringTryCatchNodeProcessor<TryCatchBlockNodeInfo>() {

    init {
        node.tryCatchBlocks.forEach { addTryNode(it) }
        node.localVariables.forEach { addLocalVarNode(it) }
    }

    fun addLocalVarNode(it: LocalVariableNode) {
        localVarsMetaInfo.addNewInterval(LocalVarNodeWrapper(it))
    }

    fun addTryNode(node: TryCatchBlockNode) {
        tryBlocksMetaInfo.addNewInterval(TryCatchBlockNodeInfo(node, false))
    }

    override fun instructionIndex(inst: AbstractInsnNode): Int {
        return node.instructions.indexOf(inst)
    }
}

public class LocalVarNodeWrapper(val node: LocalVariableNode) : Interval, SplittableInterval<LocalVarNodeWrapper> {
    override val startLabel: LabelNode
        get() = node.start
    override val endLabel: LabelNode
        get() = node.end

    override fun split(split: Interval, keepStart: Boolean): SplittedPair<LocalVarNodeWrapper> {
        val newPartInterval = if (keepStart) {
            val oldEnd = endLabel
            node.end = split.startLabel
            Pair(split.endLabel, oldEnd)
        }
        else {
            val oldStart = startLabel
            node.start = split.endLabel
            Pair(oldStart, split.startLabel)
        }

        return SplittedPair(this, LocalVarNodeWrapper(
                LocalVariableNode(node.name, node.desc, node.signature, newPartInterval.first, newPartInterval.second, node.index)
        ))
    }

}