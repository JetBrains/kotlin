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

package org.jetbrains.kotlin.ir

import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

interface IrElement {
    val startOffset: Int
    val endOffset: Int

    val parent: IrElement?
    val slot: Int
    fun setTreeLocation(newParent: IrElement?, newSlot: Int)
    fun getChild(slot: Int): IrElement?
    fun replaceChild(slot: Int, newChild: IrElement)

    fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R
    fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D): Unit
}

interface IrStatement : IrElement

abstract class IrElementBase(override val startOffset: Int, override val endOffset: Int) : IrElement {
    override var parent: IrElement? = null
    override var slot: Int = DETACHED_SLOT

    override fun setTreeLocation(newParent: IrElement?, newSlot: Int) {
        parent = newParent
        slot = newSlot
    }
}

fun <T : IrElement?> T.detach(): T {
    this?.setTreeLocation(null, DETACHED_SLOT)
    return this
}

fun IrElement.replaceWith(otherElement: IrElement) {
    if (otherElement == this) return
    val parent = this.parent ?: throw AssertionError("Can't replace a non-root element $this")
    parent.replaceChild(slot, otherElement.detach())
}

fun <T : IrElement> T.replaceWith(transformation: (T) -> IrElement) {
    val originalParent = this.parent ?: throw AssertionError("Can't replace a non-root element $this")
    val originalSlot = this.slot
    val transformed = transformation(this)
    if (transformed != this) {
        originalParent.replaceChild(originalSlot, transformed)
    }
}

fun IrElement.assertChild(child: IrElement) {
    assert(getChild(child.slot) == child) { "$this: Invalid child: $child" }
}

fun IrElement.assertDetached() {
    assert(parent == null && slot == DETACHED_SLOT) { "$this: should be detached" }
}

fun IrElement.throwNoSuchSlot(slot: Int): Nothing =
        throw AssertionError("$this: no such slot $slot")

inline fun <reified T : IrElement> IrElement.assertCast(): T =
        if (this is T) this else throw AssertionError("Expected ${T::class.simpleName}: $this")