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
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrLoop
import org.jetbrains.kotlin.ir.expressions.IrOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBase
import org.jetbrains.kotlin.types.KotlinType

abstract class IrLoopBase(
        startOffset: Int,
        endOffset: Int,
        type: KotlinType,
        override val operator: IrOperator?
) : IrExpressionBase(startOffset, endOffset, type), IrLoop {
    override var label: String? = null

    private var conditionImpl: IrExpression? = null
    override var condition: IrExpression
        get() = conditionImpl!!
        set(value) {
            conditionImpl?.detach()
            conditionImpl = value
            value.setTreeLocation(this, LOOP_CONDITION_SLOT)
        }

    override var body: IrExpression? = null
        set(value) {
            field?.detach()
            field = value
            value?.setTreeLocation(this, LOOP_BODY_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                LOOP_BODY_SLOT -> body
                LOOP_CONDITION_SLOT -> condition
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            LOOP_BODY_SLOT -> body = newChild.assertCast()
            LOOP_CONDITION_SLOT -> condition = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }
}