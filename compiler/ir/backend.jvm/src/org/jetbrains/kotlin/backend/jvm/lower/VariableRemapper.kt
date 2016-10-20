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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

abstract class AbstractVariableRemapper : IrElementTransformerVoid() {
    protected abstract fun remapVariable(value: ValueDescriptor): ValueDescriptor?

    override fun visitGetValue(expression: IrGetValue): IrExpression =
            remapVariable(expression.descriptor)?.let {
                IrGetValueImpl(expression.startOffset, expression.endOffset, it, expression.origin)
            } ?: expression
}

class VariableRemapper(val mapping: Map<ValueDescriptor, ValueDescriptor>): AbstractVariableRemapper() {
    override fun remapVariable(value: ValueDescriptor): ValueDescriptor? =
            mapping[value]
}