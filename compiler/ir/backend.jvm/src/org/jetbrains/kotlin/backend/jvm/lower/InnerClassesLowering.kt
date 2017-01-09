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
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.utils.addToStdlib.singletonList
import java.util.*

class InnerClassesLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        InnerClassTransformer(irClass).lowerInnerClass()
    }

    private inner class InnerClassTransformer(val irClass: IrClass) {
        val classDescriptor = irClass.descriptor

        lateinit var outerThisFieldDescriptor: PropertyDescriptor

        val oldConstructorParameterToNew = HashMap<ValueDescriptor, ValueDescriptor>()

        fun lowerInnerClass() {
            if (!irClass.descriptor.isInner) return

            createOuterThisField()
            lowerConstructors()
            lowerConstructorParameterUsages()
            lowerOuterThisReferences()
        }

        private fun createOuterThisField() {
            outerThisFieldDescriptor = context.specialDescriptorsFactory.getOuterThisFieldDescriptor(irClass.descriptor)

            irClass.declarations.add(IrFieldImpl(
                    irClass.startOffset, irClass.endOffset,
                    JvmLoweredDeclarationOrigin.FIELD_FOR_OUTER_THIS,
                    outerThisFieldDescriptor
            ))
        }

        private fun lowerConstructors() {
            irClass.declarations.transformFlat { irMember ->
                if (irMember is IrConstructor)
                    lowerConstructor(irMember).singletonList()
                else
                    null
            }
        }

        private fun lowerConstructor(irConstructor: IrConstructor): IrConstructor {
            val oldDescriptor = irConstructor.descriptor
            val startOffset = irConstructor.startOffset
            val endOffset = irConstructor.endOffset

            val newDescriptor = context.specialDescriptorsFactory.getInnerClassConstructorWithOuterThisParameter(oldDescriptor)
            val outerThisValueParameter = newDescriptor.valueParameters[0]

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
                                startOffset, endOffset, outerThisFieldDescriptor,
                                IrGetValueImpl(startOffset, endOffset, classDescriptor.thisAsReceiverParameter),
                                IrGetValueImpl(startOffset, endOffset, outerThisValueParameter)
                        )
                )
            }
            else {
                // Delegating constructor: invoke old constructor with dispatch receiver '$outer'
                val delegatingConstructorCall = (blockBody.statements.find { it is IrDelegatingConstructorCall } ?:
                                                 throw AssertionError("Delegating constructor call expected: ${irConstructor.dump()}")
                                                ) as IrDelegatingConstructorCall
                delegatingConstructorCall.dispatchReceiver = IrGetValueImpl(
                        delegatingConstructorCall.startOffset, delegatingConstructorCall.endOffset, outerThisValueParameter
                )
            }

            return IrConstructorImpl(
                    startOffset, endOffset,
                    irConstructor.origin, // TODO special origin for lowered inner class constructors?
                    newDescriptor,
                    blockBody
            )
        }

        private fun lowerConstructorParameterUsages() {
            irClass.transformChildrenVoid(VariableRemapper(oldConstructorParameterToNew))
        }

        private fun lowerOuterThisReferences() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val implicitThisClass = expression.descriptor.getClassDescriptorForImplicitThis() ?:
                                            return expression

                    if (implicitThisClass == classDescriptor) return expression

                    val startOffset = expression.startOffset
                    val endOffset = expression.endOffset
                    val origin = expression.origin

                    var irThis: IrExpression = IrGetValueImpl(startOffset, endOffset, classDescriptor.thisAsReceiverParameter, origin)
                    var innerClass = classDescriptor

                    while (innerClass != implicitThisClass) {
                        if (!innerClass.isInner) {
                            // Captured 'this' unrelated to inner classes nesting hierarchy, leave it as is -
                            // should be transformed by closures conversion.
                            return expression
                        }

                        val outerThisField = context.specialDescriptorsFactory.getOuterThisFieldDescriptor(innerClass)
                        irThis = IrGetFieldImpl(startOffset, endOffset, outerThisField, irThis, origin)

                        val outer = classDescriptor.containingDeclaration
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

class InnerClassConstructorCallsLowering(val context: JvmBackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val dispatchReceiver = expression.dispatchReceiver ?: return expression
                val callee = expression.descriptor as? ClassConstructorDescriptor ?: return expression
                if (!callee.constructedClass.isInner) return expression

                val newCallee = context.specialDescriptorsFactory.getInnerClassConstructorWithOuterThisParameter(callee)
                val newCall = IrCallImpl(
                        expression.startOffset, expression.endOffset, newCallee,
                        null, // TODO type arguments map
                        expression.origin
                )

                newCall.putValueArgument(0, dispatchReceiver)
                for (i in 1 .. newCallee.valueParameters.lastIndex) {
                    newCall.putValueArgument(i, expression.getValueArgument(i - 1))
                }

                return newCall
            }

            override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall): IrExpression {
                expression.transformChildrenVoid(this)

                val dispatchReceiver = expression.dispatchReceiver ?: return expression
                val callee = expression.descriptor
                if (!callee.constructedClass.isInner) return expression

                val newCallee = context.specialDescriptorsFactory.getInnerClassConstructorWithOuterThisParameter(callee)
                val newCall = IrDelegatingConstructorCallImpl(
                        expression.startOffset, expression.endOffset, newCallee,
                        null // TODO type arguments map
                )

                newCall.putValueArgument(0, dispatchReceiver)
                for (i in 1 .. newCallee.valueParameters.lastIndex) {
                    newCall.putValueArgument(i, expression.getValueArgument(i - 1))
                }

                return newCall
            }

            // TODO callable references?
        })
    }
}

