/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.inline

import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import com.google.common.collect.LinkedListMultimap
import org.jetbrains.org.objectweb.asm.Label
import java.util.ArrayList
import com.intellij.util.containers.Stack
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import com.google.common.collect.Lists
import org.jetbrains.org.objectweb.asm.tree.TryCatchBlockNode
import java.util.Comparator
import java.util.Collections

public abstract class CoveringTryCatchNodeProcessor<T: IntervalWithHandler>() {

    private val tryBlockStarts = LinkedListMultimap.create<LabelNode, T>()

    private val tryBlockEnds = LinkedListMultimap.create<LabelNode, T>()

    public val allTryCatchNodes: ArrayList<T> = arrayListOf()

    private val currentCoveringBlocks: Stack<T> = Stack()

    public val coveringFromInnermost: List<T>
        get() = currentCoveringBlocks.reverse()

    fun addNewTryCatchNode(newInfo: T) {
        tryBlockStarts.put(newInfo.startLabel, newInfo)
        tryBlockEnds.put(newInfo.endLabel, newInfo)
        allTryCatchNodes.add(newInfo)
    }

    fun remapStartLabel(oldStart: LabelNode, remapped: T) {
        tryBlockStarts.remove(oldStart, remapped)
        tryBlockStarts.put(remapped.startLabel, remapped)
    }

    public fun getStartNodes(label: LabelNode): List<T> {
        return tryBlockStarts.get(label)
    }

    public fun getEndNodes(label: LabelNode): List<T> {
        return tryBlockEnds.get(label)
    }

    //Keep information about try blocks that cover current instruction -
    // pushing and popping it to stack entering and exiting tryCatchBlock start and end labels
    public open fun updateCoveringTryBlocks(curIns: AbstractInsnNode, directOrder: Boolean) {
        if (curIns !is LabelNode) return

        val infosToClose = if (!directOrder) getStartNodes(curIns) else getEndNodes(curIns)
        for (startNode in infosToClose) {
            val pop = currentCoveringBlocks.pop()
            //Temporary disabled cause during patched structure of exceptions changed
//            if (startNode != pop) {
//                throw RuntimeException("Wrong try-catch structure " + startNode + " " + pop + " " + infosToClose.size())
//            };
        }

        //Reversing list order cause we should pop external block before internal one
        // (originally internal blocks goes before external one, such invariant preserved via sortTryCatchBlocks method)
        val infoToOpen = if (!directOrder) getEndNodes(curIns) else getStartNodes(curIns)
        for (info in infoToOpen.reverse()) {
            currentCoveringBlocks.add(info)
        }
    }

    public abstract fun instructionIndex(inst: AbstractInsnNode): Int

    private fun isEmptyInterval(node: T): Boolean {
        val start = node.startLabel
        var end: AbstractInsnNode = node.endLabel
        while (end != start && end is LabelNode) {
            end = end.getPrevious()
        }
        return start == end;
    }

    public fun getNonEmptyNodes(): List<T> {
        return allTryCatchNodes.filterNot { isEmptyInterval(it) }
    }

    public fun sortTryCatchBlocks() {
        val comp = Comparator {(t1: T, t2: T): Int ->
            var result = instructionIndex(t1.handler) - instructionIndex(t2.handler)
            if (result == 0) {
                result = instructionIndex(t1.startLabel) - instructionIndex(t2.startLabel)
                if (result == 0) {
                    assert(false, "Error: support multicatch finallies!")
                    result = instructionIndex(t1.endLabel) - instructionIndex(t2.endLabel)
                }
            }
            result
        }

        Collections.sort<T>(allTryCatchNodes, comp)
    }

}

public class DefaultProcessor(val node: MethodNode) : CoveringTryCatchNodeProcessor<TryCatchBlockNodeWrapper>() {

    {
        node.tryCatchBlocks.forEach { addNode(it) }
    }

    fun addNode(node: TryCatchBlockNode) {
        addNewTryCatchNode(TryCatchBlockNodeWrapper(node))
    }

    override fun instructionIndex(inst: AbstractInsnNode): Int {
        return node.instructions.indexOf(inst)
    }

}

public class TryCatchBlockNodeWrapper(val node: TryCatchBlockNode) : IntervalWithHandler {
    override val startLabel: LabelNode
        get() = node.start
    override val endLabel: LabelNode
        get() = node.end
    override val handler: LabelNode
        get() = node.handler
    override val type: String?
        get() = node.type
}