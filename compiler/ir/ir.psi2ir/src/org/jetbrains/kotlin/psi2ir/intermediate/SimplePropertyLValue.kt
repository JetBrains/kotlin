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

package org.jetbrains.kotlin.psi2ir.intermediate

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.Scope
import org.jetbrains.kotlin.types.KotlinType

class SimplePropertyLValue(
        val context: GeneratorContext,
        val scope: Scope,
        val startOffset: Int,
        val endOffset: Int,
        val irOperator: IrOperator?,
        val descriptor: PropertyDescriptor,
        val callReceiver: CallReceiver,
        val superQualifier: ClassDescriptor?
) : LValue, AssignmentReceiver {
    override val type: KotlinType get() = descriptor.type

    override fun load(): IrExpression =
            callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
                descriptor.getter?.let { getter ->
                    IrGetterCallImpl(startOffset, endOffset, getter,
                                     dispatchReceiverValue?.load(),
                                     extensionReceiverValue?.load(),
                                     irOperator,
                                     superQualifier)
                } ?: IrGetBackingFieldImpl(startOffset, endOffset, descriptor,
                                           dispatchReceiverValue?.load(), irOperator, superQualifier)
            }

    override fun store(irExpression: IrExpression) =
            callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
                descriptor.setter?.let { setter ->
                    IrSetterCallImpl(startOffset, endOffset, setter,
                                     dispatchReceiverValue?.load(),
                                     extensionReceiverValue?.load(),
                                     irExpression,
                                     irOperator,
                                     superQualifier)
                } ?: IrSetBackingFieldImpl(startOffset, endOffset, descriptor,
                                           dispatchReceiverValue?.load(), irExpression, irOperator, superQualifier)
            }

    override fun assign(withLValue: (LValue) -> IrExpression) =
            callReceiver.call { dispatchReceiverValue, extensionReceiverValue ->
                val dispatchReceiverTmp = dispatchReceiverValue?.let {
                    scope.createTemporaryVariable(dispatchReceiverValue.load(), "this")
                }
                val dispatchReceiverValue2 = dispatchReceiverTmp?.let { VariableLValue(it) }

                val extensionReceiverTmp = extensionReceiverValue?.let {
                    scope.createTemporaryVariable(extensionReceiverValue.load(), "receiver")
                }
                val extensionReceiverValue2 = extensionReceiverTmp?.let { VariableLValue(it) }

                val irResultExpression = withLValue(
                        SimplePropertyLValue(context, scope, startOffset, endOffset, irOperator, descriptor,
                                             SimpleCallReceiver(dispatchReceiverValue2, extensionReceiverValue2),
                                             superQualifier)
                )

                val irBlock = IrBlockImpl(startOffset, endOffset, irResultExpression.type, irOperator)
                irBlock.addIfNotNull(dispatchReceiverTmp)
                irBlock.addIfNotNull(extensionReceiverTmp)
                irBlock.addStatement(irResultExpression)
                irBlock
            }

    override fun assign(value: IrExpression): IrExpression =
            store(value)
}