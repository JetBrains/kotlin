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

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.assertDetached
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.detach
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import java.util.*

abstract class IrFunctionBase(
        startOffset: Int,
        endOffset: Int,
        origin: IrDeclarationOrigin
) : IrGeneralFunctionBase(startOffset, endOffset, origin), IrFunction {
    private val defaults = LinkedHashMap<ValueParameterDescriptor, IrExpressionBody>()

    override fun getDefault(parameter: ValueParameterDescriptor): IrExpressionBody? =
            defaults[parameter]

    override fun putDefault(parameter: ValueParameterDescriptor, expressionBody: IrExpressionBody) {
        expressionBody.assertDetached()
        defaults[parameter]?.detach()
        defaults[parameter] = expressionBody
        expressionBody.setTreeLocation(this, parameter.index)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        defaults.values.forEach { it.accept(visitor, data) }
        body?.accept(visitor, data)
    }
}