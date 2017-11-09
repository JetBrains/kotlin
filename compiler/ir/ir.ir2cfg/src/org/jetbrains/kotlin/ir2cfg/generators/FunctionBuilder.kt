/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir2cfg.generators

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir2cfg.builders.BasicBlockBuilder
import org.jetbrains.kotlin.ir2cfg.builders.BlockConnectorBuilder
import org.jetbrains.kotlin.ir2cfg.builders.ControlFlowGraphBuilder
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlock
import org.jetbrains.kotlin.ir2cfg.graph.BlockConnector
import org.jetbrains.kotlin.ir2cfg.graph.ControlFlowGraph

class FunctionBuilder(val function: IrFunction)  : ControlFlowGraphBuilder {

    private val blockBuilderMap = mutableMapOf<IrStatement, BasicBlockBuilder>()

    private var currentBlockBuilder: BasicBlockBuilder? = null

    private val blocks = mutableListOf<BasicBlock>()

    private val connectorBuilderMap = mutableMapOf<IrStatement, BlockConnectorBuilder>()

    private fun createBlockBuilder(after: BlockConnectorBuilder?): BasicBlockBuilder {
        val result = GeneralBlockBuilder(after)
        currentBlockBuilder = result
        return result
    }

    private fun BasicBlockBuilder.shiftTo(element: IrStatement) {
        blockBuilderMap.remove(last)
        add(element)
        blockBuilderMap[element] = this
    }

    override fun add(element: IrStatement) {
        val blockBuilder = currentBlockBuilder ?: createBlockBuilder(connectorBuilderMap[element])
        blockBuilder.shiftTo(element)
    }

    override fun move(to: IrStatement) {
        val blockBuilder = blockBuilderMap[to]
                           ?: connectorBuilderMap[to]?.let { createBlockBuilder(it) }
                           ?: throw AssertionError("Function generator may move to an element only to the end of a block or to connector")
        currentBlockBuilder = blockBuilder
    }

    override fun jump(to: IrStatement) {
        val blockBuilder = currentBlockBuilder
                           ?: throw AssertionError("Function generator: no default block builder for jump")
        val block = blockBuilder.build()
        blocks.add(block)
        blockBuilderMap.values.remove(blockBuilder)
        currentBlockBuilder = null
        val nextConnectorBuilder = connectorBuilderMap[to] ?: GeneralConnectorBuilder(to)
        nextConnectorBuilder.addPrevious(block)
        val previousConnectorBuilder = blockBuilder.incoming
        previousConnectorBuilder?.addNext(block)
        connectorBuilderMap[to] = nextConnectorBuilder
        move(to)
    }

    override fun jump(to: IrStatement, from: IrStatement) {
        currentBlockBuilder = blockBuilderMap[from]
        if (currentBlockBuilder == null) {
            val blockBuilder = connectorBuilderMap[from]?.let { createBlockBuilder(it) }
                               ?: throw AssertionError("Function generator may jump after an element only to the end of a block or to connector")
            currentBlockBuilder = blockBuilder
        }
        jump(to)
    }

    override fun build(): ControlFlowGraph {
        for (blockBuilder in blockBuilderMap.values) {
            if (currentBlockBuilder == blockBuilder) {
                currentBlockBuilder = null
            }
            val block = blockBuilder.build()
            blocks.add(block)
            blockBuilder.incoming?.addNext(block)
        }
        val connectors = mutableListOf<BlockConnector>()
        for (connectorBuilder in connectorBuilderMap.values) {
            connectors.add(connectorBuilder.build())
        }
        for (connector in connectors) {
            for (previous in connector.previousBlocks) {
                (previous as? BasicBlockImpl)?.outgoing = connector
            }
            for (next in connector.nextBlocks) {
                (next as? BasicBlockImpl)?.incoming = connector
            }
        }
        return ControlFlowGraphImpl(function, blocks, connectors)
    }
}
