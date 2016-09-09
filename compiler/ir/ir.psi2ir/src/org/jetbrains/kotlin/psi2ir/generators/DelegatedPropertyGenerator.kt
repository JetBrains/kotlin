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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.descriptors.IrLocalDelegatedPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrLocalDelegatedPropertyDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallableReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrThisReferenceImpl
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.builders.irBlockBody
import org.jetbrains.kotlin.psi2ir.builders.irGet
import org.jetbrains.kotlin.psi2ir.builders.irReturn
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType


class DelegatedPropertyGenerator(override val context: GeneratorContext) : Generator {
    fun generateDelegatedProperty(
            ktProperty: KtProperty,
            ktDelegate: KtPropertyDelegate,
            propertyDescriptor: PropertyDescriptor,
            irDelegateInitializer: IrExpressionBody
    ): IrDelegatedProperty {
        val delegateDescriptor = createPropertyDelegateDescriptor(ktDelegate, propertyDescriptor)

        val irDelegate = IrSimplePropertyImpl(
                ktDelegate.startOffset, ktDelegate.endOffset, IrDeclarationOrigin.DELEGATE,
                delegateDescriptor, irDelegateInitializer)

        val irProperty = IrDelegatedPropertyImpl(
                ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOrigin.DEFINED,
                propertyDescriptor, irDelegate)

        val delegateReceiverValue = createBackingFieldValueForDelegate(delegateDescriptor, ktDelegate)
        val getterDescriptor = propertyDescriptor.getter!!
        irProperty.getter = IrPropertyGetterImpl(
                ktDelegate.startOffset, ktDelegate.endOffset, IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
                getterDescriptor,
                generateDelegatedPropertyGetterBody(
                        ktDelegate, getterDescriptor, delegateReceiverValue,
                        createCallableReference(ktDelegate, delegateDescriptor.kPropertyType, propertyDescriptor)))

        if (propertyDescriptor.isVar) {
            val setterDescriptor = propertyDescriptor.setter!!
            irProperty.setter = IrPropertySetterImpl(
                    ktDelegate.startOffset, ktDelegate.endOffset, IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
                    setterDescriptor,
                    generateDelegatedPropertySetterBody(
                            ktDelegate, setterDescriptor, delegateReceiverValue,
                            createCallableReference(ktDelegate, delegateDescriptor.kPropertyType, propertyDescriptor)))
        }

        return irProperty
    }

    private fun createBackingFieldValueForDelegate(delegateDescriptor: IrPropertyDelegateDescriptor, ktDelegate: KtPropertyDelegate): IntermediateValue {
        val thisClass = delegateDescriptor.correspondingProperty.containingDeclaration as? ClassDescriptor
        val thisValue = thisClass?.let {
            RematerializableValue(IrThisReferenceImpl(ktDelegate.startOffset, ktDelegate.endOffset, thisClass.defaultType, thisClass))
        }
        return BackingFieldLValue(ktDelegate.startOffset, ktDelegate.endOffset, delegateDescriptor, thisValue, null)
    }

    private fun createCallableReference(ktElement: KtElement, type: KotlinType, referencedDescriptor: CallableDescriptor): IrCallableReference =
            IrCallableReferenceImpl(ktElement.startOffset, ktElement.endOffset, type,
                                                                        referencedDescriptor, IrOperator.PROPERTY_REFERENCE_FOR_DELEGATE)

    fun generateLocalDelegatedProperty(
            ktProperty: KtProperty,
            ktDelegate: KtPropertyDelegate,
            variableDescriptor: VariableDescriptorWithAccessors,
            irDelegateInitializer: IrExpression
    ) : IrLocalDelegatedProperty {
        val delegateDescriptor = createLocalPropertyDelegatedDescriptor(ktDelegate, variableDescriptor)

        val irDelegate = IrVariableImpl(
                ktDelegate.startOffset, ktDelegate.endOffset, IrDeclarationOrigin.DELEGATE,
                delegateDescriptor, irDelegateInitializer)

        val irLocalDelegatedProperty = IrLocalDelegatedPropertyImpl(
                ktProperty.startOffset, ktProperty.endOffset, IrDeclarationOrigin.DEFINED,
                variableDescriptor, irDelegate)

        val getterDescriptor = variableDescriptor.getter!!
        val delegateReceiverValue = createVariableValueForDelegate(delegateDescriptor, ktDelegate)
        irLocalDelegatedProperty.getter = createLocalPropertyAccessor(
                getterDescriptor, ktDelegate,
                generateDelegatedPropertyGetterBody(
                        ktDelegate, getterDescriptor, delegateReceiverValue,
                        createCallableReference(ktDelegate, delegateDescriptor.kPropertyType, delegateDescriptor.correspondingLocalProperty)))

        if (variableDescriptor.isVar) {
            val setterDescriptor = variableDescriptor.setter!!
            irLocalDelegatedProperty.setter = createLocalPropertyAccessor(
                    setterDescriptor, ktDelegate,
                    generateDelegatedPropertySetterBody(
                            ktDelegate, setterDescriptor, delegateReceiverValue,
                            createCallableReference(ktDelegate, delegateDescriptor.kPropertyType, delegateDescriptor.correspondingLocalProperty)))

        }

        return irLocalDelegatedProperty
    }

    private fun createVariableValueForDelegate(delegateDescriptor: IrLocalDelegatedPropertyDelegateDescriptor, ktDelegate: KtPropertyDelegate) =
            VariableLValue(ktDelegate.startOffset, ktDelegate.endOffset, delegateDescriptor)

    private fun createLocalPropertyAccessor(getterDescriptor: VariableAccessorDescriptor, ktDelegate: KtPropertyDelegate, body: IrBody) =
            IrLocalPropertyAccessorImpl(ktDelegate.startOffset, ktDelegate.endOffset, IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
                                                                             getterDescriptor, body)

    private fun createLocalPropertyDelegatedDescriptor(
            ktDelegate: KtPropertyDelegate,
            variableDescriptor: VariableDescriptorWithAccessors
    ): IrLocalDelegatedPropertyDelegateDescriptor {
        val delegateType = getInferredTypeWithImplicitCastsOrFail(ktDelegate.expression!!)
        val kPropertyType = context.reflectionTypes.getKPropertyType(
                Annotations.EMPTY, null, variableDescriptor.type, variableDescriptor.isVar)
        return IrLocalDelegatedPropertyDelegateDescriptorImpl(variableDescriptor, delegateType, kPropertyType)
    }

    private fun createPropertyDelegateDescriptor(
            ktDelegate: KtPropertyDelegate,
            propertyDescriptor: PropertyDescriptor
    ): IrPropertyDelegateDescriptor {
        val delegateType = getInferredTypeWithImplicitCastsOrFail(ktDelegate.expression!!)
        val propertyReceiverType = propertyDescriptor.extensionReceiverParameter?.type ?:
                                   propertyDescriptor.dispatchReceiverParameter?.type
        val kPropertyType = context.reflectionTypes.getKPropertyType(
                Annotations.EMPTY, propertyReceiverType, propertyDescriptor.type, propertyDescriptor.isVar)
        val delegateDescriptor = IrPropertyDelegateDescriptorImpl(propertyDescriptor, delegateType, kPropertyType)
        return delegateDescriptor
    }

    fun generateDelegatedPropertyGetterBody(
            ktDelegate: KtPropertyDelegate,
            getterDescriptor: VariableAccessorDescriptor,
            delegateReceiverValue: IntermediateValue,
            irPropertyReference: IrCallableReference
    ): IrBody = with(BodyGenerator(getterDescriptor, context)) {
        irBlockBody(ktDelegate) {
            val statementGenerator = createStatementGenerator()
            val conventionMethodResolvedCall = getOrFail(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor)
            val conventionMethodCall = statementGenerator.pregenerateCall(conventionMethodResolvedCall)
            conventionMethodCall.setExplicitReceiverValue(delegateReceiverValue)
            conventionMethodCall.irValueArgumentsByIndex[1] = irPropertyReference
            +irReturn(CallGenerator(statementGenerator).generateCall(ktDelegate.startOffset, ktDelegate.endOffset, conventionMethodCall))
        }
    }

    fun generateDelegatedPropertySetterBody(
            ktDelegate: KtPropertyDelegate,
            setterDescriptor: VariableAccessorDescriptor,
            delegateReceiverValue: IntermediateValue,
            irPropertyReference: IrCallableReference
    ): IrBody = with(BodyGenerator(setterDescriptor, context)) {
        irBlockBody(ktDelegate) {
            val statementGenerator = createStatementGenerator()
            val conventionMethodResolvedCall = getOrFail(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, setterDescriptor)
            val conventionMethodCall = statementGenerator.pregenerateCall(conventionMethodResolvedCall)
            conventionMethodCall.setExplicitReceiverValue(delegateReceiverValue)
            conventionMethodCall.irValueArgumentsByIndex[1] = irPropertyReference
            conventionMethodCall.irValueArgumentsByIndex[2] = irGet(setterDescriptor.valueParameters[0])
            +irReturn(CallGenerator(statementGenerator).generateCall(ktDelegate.startOffset, ktDelegate.endOffset, conventionMethodCall))
        }
    }
}