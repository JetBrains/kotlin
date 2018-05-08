/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.createParameterDeclarations
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import java.util.*

class InnerClassesLowering(val context: BackendContext) : ClassLoweringPass {
    object FIELD_FOR_OUTER_THIS : IrDeclarationOriginImpl("FIELD_FOR_OUTER_THIS")

    override fun lower(irClass: IrClass) {
        InnerClassTransformer(irClass).lowerInnerClass()
    }

    private inner class InnerClassTransformer(val irClass: IrClass) {
        lateinit var outerThisField: IrField

        val oldConstructorParameterToNew = HashMap<ValueDescriptor, IrValueParameter>()
        val class2Symbol = HashMap<ClassDescriptor, IrClass>()

        fun lowerInnerClass() {
            if (!irClass.descriptor.isInner) return
            rememberClassSymbols()

            createOuterThisField()
            lowerConstructors()
            lowerConstructorParameterUsages()
            lowerOuterThisReferences()
        }

        //TODO: rewrite: this methods is required to 'getClassForImplicitThis' method
        private fun rememberClassSymbols() {
            var current = irClass.parent as? IrClass
            while (current != null) {
                class2Symbol[current.descriptor] = current
                current = current.parent as? IrClass
            }
            irClass.acceptVoid(object : IrElementVisitorVoid {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitClass(declaration: IrClass) {
                    return super.visitClass(declaration).also { class2Symbol[declaration.descriptor] = declaration }
                }
            })
        }

        private fun createOuterThisField() {
            irClass.declarations.add(
                IrFieldImpl(
                    irClass.startOffset, irClass.endOffset,
                    FIELD_FOR_OUTER_THIS,
                    context.descriptorsFactory.getOuterThisFieldSymbol(irClass)
                ).also {
                    outerThisField = it
                }
            )
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

            val newSymbol = context.descriptorsFactory.getInnerClassConstructorWithOuterThisParameter(oldDescriptor)
            val loweredConstructor = IrConstructorImpl(
                startOffset, endOffset,
                irConstructor.origin, // TODO special origin for lowered inner class constructors?
                newSymbol,
                null
            )
            loweredConstructor.createParameterDeclarations()
            val outerThisValueParameter = loweredConstructor.valueParameters[0].symbol

            oldDescriptor.valueParameters.forEach { oldValueParameter ->
                oldConstructorParameterToNew[oldValueParameter] = loweredConstructor.valueParameters[oldValueParameter.index + 1]
            }

            val blockBody = irConstructor.body as? IrBlockBody ?: throw AssertionError("Unexpected constructor body: ${irConstructor.body}")

            val instanceInitializerIndex = blockBody.statements.indexOfFirst { it is IrInstanceInitializerCall }
            if (instanceInitializerIndex >= 0) {
                // Initializing constructor: initialize 'this.this$0' with '$outer'
                blockBody.statements.add(
                    instanceInitializerIndex,
                    IrSetFieldImpl(
                        startOffset, endOffset, outerThisField.symbol,
                        IrGetValueImpl(startOffset, endOffset, irClass.thisReceiver!!.symbol),
                        IrGetValueImpl(startOffset, endOffset, outerThisValueParameter)
                    )
                )
            } else {
                // Delegating constructor: invoke old constructor with dispatch receiver '$outer'
                val delegatingConstructorCall = (blockBody.statements.find { it is IrDelegatingConstructorCall }
                        ?: throw AssertionError("Delegating constructor call expected: ${irConstructor.dump()}")
                        ) as IrDelegatingConstructorCall
                delegatingConstructorCall.dispatchReceiver = IrGetValueImpl(
                    delegatingConstructorCall.startOffset, delegatingConstructorCall.endOffset, outerThisValueParameter
                )
            }

            loweredConstructor.body = blockBody
            return loweredConstructor
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

                    val implicitThisClass = expression.symbol.getClassForImplicitThis() ?: return expression

                    if (implicitThisClass == irClass) return expression

                    val startOffset = expression.startOffset
                    val endOffset = expression.endOffset
                    val origin = expression.origin

                    var irThis: IrExpression = IrGetValueImpl(startOffset, endOffset, irClass.thisReceiver!!.symbol, origin)
                    var innerClass = irClass

                    while (innerClass != implicitThisClass) {
                        if (!innerClass.descriptor.isInner) {
                            // Captured 'this' unrelated to inner classes nesting hierarchy, leave it as is -
                            // should be transformed by closures conversion.
                            return expression
                        }

                        val outerThisField = context.descriptorsFactory.getOuterThisFieldSymbol(innerClass)
                        irThis = IrGetFieldImpl(startOffset, endOffset, outerThisField, irThis, origin)

                        val outer = innerClass.parent
                        innerClass = outer as? IrClass ?:
                                throw AssertionError("Unexpected containing declaration for inner class $innerClass: $outer")
                    }

                    return irThis
                }
            })
        }

        private fun IrValueSymbol.getClassForImplicitThis(): IrClass? {
            val descriptor1 = this.descriptor
            if (descriptor1 is ReceiverParameterDescriptor) {
                val receiverValue = descriptor1.value
                if (receiverValue is ImplicitClassReceiver) {
                    return class2Symbol[receiverValue.classDescriptor]!!
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
                    expression.startOffset, expression.endOffset, newCallee, newCallee.descriptor,
                    null, // TODO type arguments map
                    expression.origin
                )

                newCall.putValueArgument(0, dispatchReceiver)
                for (i in 1..newCallee.descriptor.valueParameters.lastIndex) {
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
                    expression.startOffset, expression.endOffset, newCallee, newCallee.descriptor,
                    null // TODO type arguments map
                )

                newCall.putValueArgument(0, dispatchReceiver)
                for (i in 1..newCallee.descriptor.valueParameters.lastIndex) {
                    newCall.putValueArgument(i, expression.getValueArgument(i - 1))
                }

                return newCall
            }

            // TODO callable references?
        })
    }
}

