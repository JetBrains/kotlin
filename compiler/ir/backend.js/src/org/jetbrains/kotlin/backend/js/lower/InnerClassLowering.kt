/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.js.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver

class InnerClassLowering(private val descriptorProvider: InnerClassDescriptorProvider) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.descriptor.isInner) return

        InnerClassTransformer(irClass).apply()
    }

    private inner class InnerClassTransformer(val irClass: IrClass) {
        val oldConstructorParameterToNew = hashMapOf<ValueParameterDescriptor, ValueParameterDescriptor>()

        fun apply() {
            addOuterField()
            lowerConstructors()
            lowerConstructorParameterUsages()
            lowerOuterThisReferences()
        }

        private fun addOuterField() {
            val fieldDescriptor = descriptorProvider.getOuterProperty(irClass.descriptor)
            val irField = IrFieldImpl(
                    irClass.startOffset, irClass.endOffset, irClass.origin,
                    IrFieldSymbolImpl(fieldDescriptor))
            irClass.declarations += irField
        }

        private fun lowerConstructors() {
            irClass.declarations.transformFlat { irMember ->
                if (irMember is IrConstructor)
                    listOf(lowerConstructor(irMember))
                else
                    null
            }
        }

        private fun lowerConstructor(irConstructor: IrConstructor): IrConstructor {
            val oldDescriptor = irConstructor.descriptor
            val startOffset = irConstructor.startOffset
            val endOffset = irConstructor.endOffset

            val newDescriptor = descriptorProvider.getConstructor(oldDescriptor)
            val outerThisValueParameter = newDescriptor.valueParameters[0]
            val outerThisFieldDescriptor = descriptorProvider.getOuterProperty(irClass.descriptor)
            val classDescriptor = irClass.descriptor

            oldDescriptor.valueParameters.forEach { oldValueParameter ->
                oldConstructorParameterToNew[oldValueParameter] = newDescriptor.valueParameters[oldValueParameter.index + 1]
            }

            val blockBody = irConstructor.body as? IrBlockBody ?: throw AssertionError("Unexpected constructor body: ${irConstructor.body}")

            val instanceInitializerIndex = blockBody.statements.indexOfFirst { it is IrInstanceInitializerCall }
            if (instanceInitializerIndex >= 0) {
                // Initializing constructor: initialize 'this.this$0' with '$outer'
                blockBody.statements.add(
                        instanceInitializerIndex,
                        IrSetFieldImpl(
                                startOffset, endOffset, IrFieldSymbolImpl(outerThisFieldDescriptor),
                                IrGetValueImpl(startOffset, endOffset, IrValueParameterSymbolImpl(classDescriptor.thisAsReceiverParameter)),
                                IrGetValueImpl(startOffset, endOffset, IrValueParameterSymbolImpl(outerThisValueParameter))
                        )
                )
            }
            else {
                // Delegating constructor: invoke old constructor with dispatch receiver '$outer'
                val delegatingConstructorCall = (blockBody.statements.find { it is IrDelegatingConstructorCall } ?:
                                                 throw AssertionError("Delegating constructor call expected: ${irConstructor.dump()}")
                                                ) as IrDelegatingConstructorCall
                delegatingConstructorCall.dispatchReceiver = IrGetValueImpl(
                        delegatingConstructorCall.startOffset, delegatingConstructorCall.endOffset,
                        IrValueParameterSymbolImpl(outerThisValueParameter)
                )
            }

            val newConstructor = IrConstructorImpl(
                    startOffset, endOffset,
                    irConstructor.origin, // TODO special origin for lowered inner class constructors?
                    newDescriptor,
                    blockBody
            )

            newConstructor.valueParameters += IrValueParameterImpl(
                    irConstructor.startOffset, irConstructor.endOffset, irConstructor.origin,
                    IrValueParameterSymbolImpl(newDescriptor.valueParameters[0]))
            newConstructor.valueParameters += irConstructor.valueParameters

            return newConstructor
        }

        private fun lowerConstructorParameterUsages() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    val newDescriptor = oldConstructorParameterToNew[expression.descriptor] ?: return expression
                    return IrGetValueImpl(
                            expression.startOffset, expression.endOffset, IrValueParameterSymbolImpl(newDescriptor), expression.origin)
                }
            })
        }


        private fun lowerOuterThisReferences() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)
                    val classDescriptor = irClass.descriptor

                    val implicitThisClass = expression.descriptor.getClassDescriptorForImplicitThis() ?:
                                            return expression

                    if (implicitThisClass == classDescriptor) return expression

                    val startOffset = expression.startOffset
                    val endOffset = expression.endOffset
                    val origin = expression.origin

                    var irThis: IrExpression = IrGetValueImpl(
                            startOffset, endOffset,
                            IrValueParameterSymbolImpl(classDescriptor.thisAsReceiverParameter), origin)
                    var innerClass = classDescriptor

                    while (innerClass != implicitThisClass) {
                        if (!innerClass.isInner) {
                            // Captured 'this' unrelated to inner classes nesting hierarchy, leave it as is -
                            // should be transformed by closures conversion.
                            return expression
                        }

                        val outerThisField = descriptorProvider.getOuterProperty(innerClass)
                        irThis = IrGetFieldImpl(startOffset, endOffset, IrFieldSymbolImpl(outerThisField), irThis, origin)

                        val outer = innerClass.containingDeclaration
                        innerClass = outer as? ClassDescriptor ?:
                                     throw AssertionError("Unexpected containing declaration for inner class $innerClass: $outer")
                    }

                    return irThis
                }
            })
        }

        private fun ValueDescriptor.getClassDescriptorForImplicitThis(): ClassDescriptor? {
            if (this is ReceiverParameterDescriptor) {
                val receiverValue = value
                if (receiverValue is ImplicitClassReceiver) {
                    return receiverValue.classDescriptor
                }
            }
            return null
        }
    }
}