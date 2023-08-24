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

package org.jetbrains.kotlin.ir2cfg.nodes

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerShallow
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorShallow

class MergeCfgElement(val from: IrElement, val name: String) : CfgIrElement {
    override val startOffset = from.startOffset
    override val endOffset = from.endOffset

    override fun <R, D> accept(visitor: IrElementVisitorShallow<R, D>, data: D) = visitor.visitElement(this, data)

    override fun <D> transform(transformer: IrElementTransformerShallow<D>, data: D): IrElement = accept(transformer, data)

    override fun <D> acceptChildren(visitor: IrElementVisitorShallow<Unit, D>, data: D) = Unit

    override fun <D> transformChildren(transformer: IrElementTransformerShallow<D>, data: D) = Unit

    override fun toString() = "$name: ${from.dump()}"
}
