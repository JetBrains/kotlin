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

package org.jetbrains.kotlin.ir.expressions.persistent

import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrBodyBase
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.declarations.persistent.carriers.Carrier
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody

internal class PersistentIrExpressionBody private constructor(
    override val startOffset: Int,
    override val endOffset: Int,
    override val factory: PersistentIrFactory,
    private var expressionField: IrExpression? = null,
    override var initializer: (PersistentIrExpressionBody.() -> Unit)? = null,
) : IrExpressionBody(), PersistentIrBodyBase<PersistentIrExpressionBody> {
    override var lastModified: Int = factory.stageController.currentStage
    override var loweredUpTo: Int = factory.stageController.currentStage
    override var values: Array<Carrier>? = null
    override val createdOn: Int = factory.stageController.currentStage

    override var containerField: IrDeclaration? = null

    constructor(expression: IrExpression, factory: PersistentIrFactory) : this(expression.startOffset, expression.endOffset, factory, expression, null)

    constructor(startOffset: Int, endOffset: Int, expression: IrExpression,factory: PersistentIrFactory) : this(startOffset, endOffset, factory, expression, null)

    constructor(startOffset: Int, endOffset: Int, factory: PersistentIrFactory, initializer: IrExpressionBody.() -> Unit) :
            this(startOffset, endOffset, factory, null, initializer)

    override var expression: IrExpression
        get() = checkEnabled { expressionField!! }
        set(e) {
            checkEnabled { expressionField = e }
        }

    private val hashCodeValue: Int = PersistentIrDeclarationBase.hashCodeCounter++
    override fun hashCode(): Int = hashCodeValue
    override fun equals(other: Any?): Boolean = (this === other)
}
