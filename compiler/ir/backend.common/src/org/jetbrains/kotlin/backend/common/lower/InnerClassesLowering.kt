/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import java.util.*

class InnerClassesLowering(val context: BackendContext) : ClassLoweringPass {
    object FIELD_FOR_OUTER_THIS : IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")

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
            outerThisFieldDescriptor = context.descriptorsFactory.getOuterThisFieldDescriptor(irClass.descriptor)

            irClass.declarations.add(IrFieldImpl(
                irClass.startOffset, irClass.endOffset,
                FIELD_FOR_OUTER_THIS,
                outerThisFieldDescriptor
            ))
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

            val newDescriptor = context.descriptorsFactory.getInnerClassConstructorWithOuterThisParameter(oldDescriptor)
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
            ).apply {
                newDescriptor.valueParameters.forEachIndexed { i, desc ->
                    val valueParameter = if (i == 0) {
                        IrValueParameterImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, FIELD_FOR_OUTER_THIS, desc, null)
                    } else {
                        val origParam = irConstructor.valueParameters[i - 1]
                        IrValueParameterImpl(origParam.startOffset, origParam.endOffset, origParam.origin, desc, origParam.defaultValue)
                    }
                    valueParameters.add(valueParameter)
                }
            }
        }

        private fun lowerConstructorParameterUsages() {
            irClass.transformChildrenVoid(VariableRemapper(oldConstructorParameterToNew))
        }

        private fun lowerOuterThisReferences() {
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {


                override fun visitClass(declaration: IrClass): IrStatement =
                        //TODO: maybe add another transformer that skips specified elements
                        declaration

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

                        val outerThisField = context.descriptorsFactory.getOuterThisFieldDescriptor(innerClass)
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

class InnerClassConstructorCallsLowering(val context: BackendContext) : BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)

                val dispatchReceiver = expression.dispatchReceiver ?: return expression
                val callee = expression.descriptor as? ClassConstructorDescriptor ?: return expression
                if (!callee.constructedClass.isInner) return expression

                val newCallee = context.descriptorsFactory.getInnerClassConstructorWithOuterThisParameter(callee)
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

                val newCallee = context.descriptorsFactory.getInnerClassConstructorWithOuterThisParameter(callee)
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

