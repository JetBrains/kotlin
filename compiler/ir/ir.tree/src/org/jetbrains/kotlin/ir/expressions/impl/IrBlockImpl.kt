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

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.expressions.IrBlock
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.types.KotlinType
import java.util.*

class IrBlockImpl(startOffset: Int, endOffset: Int, type: KotlinType, override val operator: IrOperator? = null):
        IrExpressionBase(startOffset, endOffset, type), IrBlock {
    constructor(startOffset: Int, endOffset: Int, type: KotlinType, operator: IrOperator?, statements: List<IrStatement>) :
            this(startOffset, endOffset, type, operator) {
        addAll(statements)
    }

    override val statements: MutableList<IrStatement> = ArrayList(2)

    fun addStatement(statement: IrStatement) {
        statement.assertDetached()
        statement.setTreeLocation(this, statements.size)
        statements.add(statement)
    }

    fun addAll(newStatements: List<IrStatement>) {
        newStatements.forEach { it.assertDetached() }
        val originalSize = this.statements.size
        this.statements.addAll(newStatements)
        newStatements.forEachIndexed { i, irStatement ->
            irStatement.setTreeLocation(this, originalSize + i)
        }
    }

    override fun getChild(slot: Int): IrElement? =
            statements.getOrNull(slot)

    override fun replaceChild(slot: Int, newChild: IrElement) {
        if (slot < 0 || slot >= statements.size) throwNoSuchSlot(slot)

        newChild.assertDetached()
        statements[slot].detach()
        statements[slot] = newChild.assertCast()
        newChild.setTreeLocation(this, slot)
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitBlock(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        statements.forEach { it.accept(visitor, data) }
    }
}

fun IrBlockImpl.addIfNotNull(statement: IrStatement?) {
    if (statement != null) addStatement(statement)
}

fun IrBlockImpl.inlineStatement(statement: IrStatement) {
    if (statement is IrBlock) {
        statement.statements.forEach { it.detach() }
        addAll(statement.statements)
    }
    else {
        addStatement(statement)
    }
}