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
import org.jetbrains.kotlin.ir2cfg.builders.BlockConnectorBuilder
import org.jetbrains.kotlin.ir2cfg.graph.BasicBlock
import org.jetbrains.kotlin.utils.toReadOnlyList

class GeneralConnectorBuilder(private val element: IrStatement) : BlockConnectorBuilder {

    private val next = linkedSetOf<BasicBlock>()

    private val previous = linkedSetOf<BasicBlock>()

    override fun addNext(basicBlock: BasicBlock) {
        next.add(basicBlock)
    }

    override fun addPrevious(basicBlock: BasicBlock) {
        previous.add(basicBlock)
    }

    override fun build() = when {
        next.size <= 1 -> JoinBlockConnector(previous.toReadOnlyList(), element, next.firstOrNull())
        previous.size == 1 -> SplitBlockConnector(previous.single(), element, next.toReadOnlyList())
        else -> throw AssertionError("Connector should have either exactly one previous block or no more than one next block, " +
                                     "actual previous = ${previous.size}, next = ${next.size}")
    }
}