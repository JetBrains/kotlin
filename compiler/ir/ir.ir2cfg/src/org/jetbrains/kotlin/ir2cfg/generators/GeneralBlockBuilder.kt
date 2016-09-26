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
import org.jetbrains.kotlin.ir2cfg.builders.BasicBlockBuilder
import org.jetbrains.kotlin.ir2cfg.builders.BlockConnectorBuilder
import org.jetbrains.kotlin.utils.toReadOnlyList

class GeneralBlockBuilder(override val incoming: BlockConnectorBuilder?) : BasicBlockBuilder {

    private val elements = mutableListOf<IrStatement>()

    override fun add(element: IrStatement) {
        elements.add(element)
    }

    override val last: IrStatement?
        get() = elements.lastOrNull()

    override fun build() = BasicBlockImpl(elements.toReadOnlyList())
}