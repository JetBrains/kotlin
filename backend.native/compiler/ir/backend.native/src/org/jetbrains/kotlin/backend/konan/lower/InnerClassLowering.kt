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

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.lower.callsSuper
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver

internal class InnerClassLowering(val context: Context) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        InnerClassTransformer(irClass).lowerInnerClass()
    }

    private inner class InnerClassTransformer(val irClass: IrClass) {
        val classDescriptor = irClass.descriptor

        lateinit var outerThisFieldSymbol: IrFieldSymbol

        fun lowerInnerClass() {
            if (!irClass.descriptor.isInner) return

            createOuterThisField()
            lowerOuterThisReferences()
            lowerConstructors()
        }

        private fun createOuterThisField() {
            val field = context.specialDeclarationsFactory.getOuterThisField(irClass)
            outerThisFieldSymbol = field.symbol
            irClass.addChild(field)
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
            if (irConstructor.callsSuper()) {
                // Initializing constructor: initialize 'this.this$0' with '$outer'.
                val blockBody = irConstructor.body as? IrBlockBody
                        ?: throw AssertionError("Unexpected constructor body: ${irConstructor.body}")
                val startOffset = irConstructor.startOffset
                val endOffset = irConstructor.endOffset
                val thisReceiver = irClass.thisReceiver!!
                val outerReceiver = irConstructor.dispatchReceiverParameter!!
                blockBody.statements.add(
                        0,
                        IrSetFieldImpl(
                                startOffset, endOffset, outerThisFieldSymbol,
                                IrGetValueImpl(startOffset, endOffset, thisReceiver.type, thisReceiver.symbol),
                                IrGetValueImpl(startOffset, endOffset, outerReceiver.type, outerReceiver.symbol),
                                context.irBuiltIns.unitType
                        )
                )
            }

            return irConstructor
        }

        private fun lowerOuterThisReferences() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val implicitThisClass = expression.descriptor.getClassDescriptorForImplicitThis() ?:
                            return expression

                    if (implicitThisClass == classDescriptor) return expression

                    val constructorSymbol = currentFunction!!.scope.scopeOwnerSymbol as? IrConstructorSymbol

                    val startOffset = expression.startOffset
                    val endOffset = expression.endOffset
                    val origin = expression.origin

                    var irThis: IrExpression
                    var innerClass: IrClass
                    if (constructorSymbol == null || constructorSymbol.descriptor.constructedClass != classDescriptor) {
                        innerClass = irClass
                        val currentIrFunction = currentFunction!!.scope.scopeOwnerSymbol.owner as IrFunction

                        val currentFunctionReceiver = currentIrFunction.dispatchReceiverParameter
                        val thisParameter = if (currentFunctionReceiver?.descriptor == irClass.thisReceiver!!.descriptor) {
                            currentFunctionReceiver
                        } else {
                            irClass.thisReceiver!!
                        }

                        irThis = IrGetValueImpl(startOffset, endOffset, thisParameter.type, thisParameter.symbol, origin)
                    } else {
                        // For constructor we have outer class as dispatchReceiverParameter.
                        innerClass = irClass.parent as? IrClass ?:
                                throw AssertionError("No containing class for inner class $classDescriptor")
                        val thisParameter = constructorSymbol.owner.dispatchReceiverParameter!!
                        irThis = IrGetValueImpl(startOffset, endOffset, thisParameter.type, thisParameter.symbol, origin)
                    }

                    while (innerClass.descriptor != implicitThisClass) {
                        if (!innerClass.isInner) {
                            // Captured 'this' unrelated to inner classes nesting hierarchy, leave it as is -
                            // should be transformed by closures conversion.
                            return expression
                        }

                        val outerThisField = context.specialDeclarationsFactory.getOuterThisField(innerClass)
                        irThis = IrGetFieldImpl(
                                startOffset, endOffset,
                                outerThisField.symbol, outerThisField.type,
                                irThis,
                                origin
                        )

                        val outer = innerClass.parent
                        innerClass = outer as? IrClass ?:
                                throw AssertionError("Unexpected containing declaration for inner class ${innerClass.descriptor}: $outer")
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

