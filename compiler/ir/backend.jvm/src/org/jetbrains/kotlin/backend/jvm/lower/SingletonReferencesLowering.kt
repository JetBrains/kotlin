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

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class SingletonReferencesLowering(val context: JvmBackendContext) : BodyLoweringPass, IrElementTransformerVoid() {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(this)
    }

    override fun visitGetEnumValue(expression: IrGetEnumValue): IrExpression {
        val enumValueFieldDescriptor = context.specialDescriptorsFactory.getFieldDescriptorForEnumEntry(expression.descriptor)
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, enumValueFieldDescriptor)
    }

    override fun visitGetObjectValue(expression: IrGetObjectValue): IrExpression {
        val instanceFieldDescriptor = context.specialDescriptorsFactory.getFieldDescriptorForObjectInstance(expression.descriptor)
        return IrGetFieldImpl(expression.startOffset, expression.endOffset, instanceFieldDescriptor)
    }
}