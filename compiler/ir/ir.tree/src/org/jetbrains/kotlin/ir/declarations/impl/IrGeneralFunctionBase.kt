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

package org.jetbrains.kotlin.ir.declarations.impl

import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrGeneralFunctionBase(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin
) : IrDeclarationBase(startOffset, endOffset, origin), IrFunction {
    final override var body: IrBody? = null
        set(newValue) {
            field?.detach()
            field = newValue
            newValue?.setTreeLocation(this, FUNCTION_BODY_SLOT)
        }

    override fun getChild(slot: Int): IrElement? =
            when (slot) {
                FUNCTION_BODY_SLOT -> body
                else -> null
            }

    override fun replaceChild(slot: Int, newChild: IrElement) {
        when (slot) {
            FUNCTION_BODY_SLOT -> body = newChild.assertCast()
            else -> throwNoSuchSlot(slot)
        }
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        body?.accept(visitor, data)
    }
}