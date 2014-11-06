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

import java.util.ArrayList
import org.jetbrains.org.objectweb.asm.tree.TryCatchBlockNode
import org.jetbrains.org.objectweb.asm.tree.LabelNode
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.jet.codegen.inline.InlineCodegenUtil.*
import kotlin.properties.Delegates

enum class TryCatchPosition {
    START
    END
    INNER
}

trait IntervalWithHandler {
    val startLabel: LabelNode
    val endLabel: LabelNode
    val handler: LabelNode
    val type: String?
}

class TryCatchBlockNodeInfo(val node: TryCatchBlockNode, val onlyCopyNotProcess: Boolean) : IntervalWithHandler {

    override val startLabel: LabelNode
            get() = node.start
    override val endLabel: LabelNode
            get() = node.end
    override val handler: LabelNode
            get() = node.handler
    override val type: String?
            get() = node.type
}

class TryCatchBlockNodePosition(val nodeInfo: TryCatchBlockNodeInfo, var position: TryCatchPosition): IntervalWithHandler by nodeInfo

class TryBlockCluster<T : IntervalWithHandler>(val blocks: MutableList<T>) {
    val defaultHandler: T?
        get() = blocks.firstOrNull() { it.type == null }
}


fun <T: IntervalWithHandler> doClustering(blocks: List<T>) : List<TryBlockCluster<T>> {
    [data] class TryBlockInterval(val startLabel: LabelNode, val endLabel: LabelNode)

    val clusters = linkedMapOf<TryBlockInterval, TryBlockCluster<T>>()
    blocks.forEach { block ->
        val interval = TryBlockInterval(firstLabelInChain(block.startLabel), firstLabelInChain(block.endLabel))
        val cluster = clusters.getOrPut(interval, {TryBlockCluster(arrayListOf())})
        cluster.blocks.add(block)
    }

    return clusters.values().toList();
}

