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

enum class TryCatchPosition {
    START
    END
    INNER
}

class TryCatchBlockNodeInfo(val node: TryCatchBlockNode, val onlyCopyNotProcess: Boolean)

class TryCatchBlockNodePosition(val nodeInfo: TryCatchBlockNodeInfo, var position: TryCatchPosition)

class TryBlockCluster(val blocks: MutableList<TryCatchBlockNodeInfo>)


fun doClustering(blocks: List<TryCatchBlockNodeInfo>) : List<TryBlockCluster> {
    [data] class TryBlockInterval(val startLabel: LabelNode, val endLabel: LabelNode)

    val clusters = linkedMapOf<TryBlockInterval, TryBlockCluster>()
    blocks.forEach { block ->
        val interval = TryBlockInterval(firstLabelInChain(block.node.start), firstLabelInChain(block.node.end))
        val cluster = clusters.getOrPut(interval, {TryBlockCluster(arrayListOf())})
        cluster.blocks.add(block)
    }

    return clusters.values().toList();
}

