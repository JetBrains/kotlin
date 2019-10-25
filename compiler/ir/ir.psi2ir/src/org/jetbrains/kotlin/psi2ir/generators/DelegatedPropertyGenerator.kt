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
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.descriptors.IrLocalDelegatedPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrLocalDelegatedPropertyDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptor
import org.jetbrains.kotlin.ir.descriptors.IrPropertyDelegateDescriptorImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrExpressionBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffsetSkippingComments
import org.jetbrains.kotlin.psi2ir.intermediate.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType

class DelegatedPropertyGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {
    constructor(context: GeneratorContext) : this(DeclarationGenerator(context))

    fun generateDelegatedProperty(
        ktProperty: KtProperty,
        ktDelegate: KtPropertyDelegate,
        propertyDescriptor: PropertyDescriptor
    ): IrProperty {

        val kPropertyType = getKPropertyTypeForDelegatedProperty(propertyDescriptor)

        val irProperty = context.symbolTable.declareProperty(
            ktProperty.startOffsetSkippingComments, ktProperty.endOffset, IrDeclarationOrigin.DEFINED,
            propertyDescriptor,
            isDelegated = true
        ).apply {
            backingField = generateDelegateFieldForProperty(propertyDescriptor, kPropertyType, ktDelegate)
        }

        val irDelegate = irProperty.backingField!!

        val thisClass = propertyDescriptor.containingDeclaration as? ClassDescriptor
        val delegateReceiverValue = createBackingFieldValueForDelegate(irDelegate.symbol, thisClass, ktDelegate)
        val getterDescriptor = propertyDescriptor.getter!!
        irProperty.getter = generateDelegatedPropertyAccessor(ktProperty, ktDelegate, getterDescriptor) { irGetter ->
            generateDelegatedPropertyGetterBody(
                irGetter,
                ktDelegate, getterDescriptor, delegateReceiverValue,
                createCallableReference(ktDelegate, kPropertyType, propertyDescriptor, irGetter.symbol)
            )
        }

        if (propertyDescriptor.isVar) {
            val setterDescriptor = propertyDescriptor.setter!!
            irProperty.setter = generateDelegatedPropertyAccessor(ktProperty, ktDelegate, setterDescriptor) { irSetter ->
                generateDelegatedPropertySetterBody(
                    irSetter,
                    ktDelegate, setterDescriptor, delegateReceiverValue,
                    createCallableReference(ktDelegate, kPropertyType, propertyDescriptor, irSetter.symbol)
                )
            }
        }

        return irProperty
    }

    private inline fun generateDelegatedPropertyAccessor(
        ktProperty: KtProperty,
        ktDelegate: KtPropertyDelegate,
        accessorDescriptor: PropertyAccessorDescriptor,
        generateBody: (IrFunction) -> IrBody
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset,
            IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
            accessorDescriptor
        ).buildWithScope { irAccessor ->
            FunctionGenerator(declarationGenerator).generateFunctionParameterDeclarationsAndReturnType(irAccessor, ktProperty, null)
            irAccessor.body = generateBody(irAccessor)
        }


    private fun getKPropertyTypeForDelegatedProperty(propertyDescriptor: PropertyDescriptor): KotlinType {
        val receivers = listOfNotNull(propertyDescriptor.extensionReceiverParameter, propertyDescriptor.dispatchReceiverParameter)
        return context.reflectionTypes.getKPropertyType(
            Annotations.EMPTY,
            receivers.map { it.type },
            propertyDescriptor.type,
            propertyDescriptor.isVar
        )
    }

    private fun generateDelegateFieldForProperty(
        propertyDescriptor: PropertyDescriptor,
        kPropertyType: KotlinType,
        ktDelegate: KtPropertyDelegate
    ): IrField {
        val delegateType = getDelegatedPropertyDelegateType(propertyDescriptor, ktDelegate)
        val delegateDescriptor = createPropertyDelegateDescriptor(propertyDescriptor, delegateType, kPropertyType)

        val startOffset = ktDelegate.startOffsetSkippingComments
        val endOffset = ktDelegate.endOffset
        val origin = IrDeclarationOrigin.DELEGATE
        val type = delegateDescriptor.type.toIrType()
        return context.symbolTable.declareField(
            startOffset, endOffset, origin, delegateDescriptor, type
        ) {
            IrFieldImpl(startOffset, endOffset, origin, it, type).apply {
                metadata = MetadataSource.Property(propertyDescriptor)
            }
        }.also { irDelegate ->
            irDelegate.initializer = generateInitializerBodyForPropertyDelegate(
                propertyDescriptor, kPropertyType, ktDelegate,
                irDelegate.symbol
            )
        }
    }

    private fun generateInitializerBodyForPropertyDelegate(
        property: VariableDescriptorWithAccessors,
        kPropertyType: KotlinType,
        ktDelegate: KtPropertyDelegate,
        scopeOwner: IrSymbol
    ): IrExpressionBody {
        val ktDelegateExpression = ktDelegate.expression!!
        val irDelegateInitializer = declarationGenerator.generateInitializerBody(scopeOwner, ktDelegateExpression)

        val provideDelegateResolvedCall = get(BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL, property)
            ?: return irDelegateInitializer

        val statementGenerator = createBodyGenerator(scopeOwner).createStatementGenerator()
        val provideDelegateCall = statementGenerator.pregenerateCall(provideDelegateResolvedCall)
        provideDelegateCall.setExplicitReceiverValue(OnceExpressionValue(irDelegateInitializer.expression))
        provideDelegateCall.irValueArgumentsByIndex[1] = createCallableReference(ktDelegate, kPropertyType, property, scopeOwner)
        val irProvideDelegate = CallGenerator(statementGenerator).generateCall(
            ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset, provideDelegateCall
        )
        return IrExpressionBodyImpl(irProvideDelegate)
    }

    private fun createBackingFieldValueForDelegate(
        irDelegateField: IrFieldSymbol,
        thisClass: ClassDescriptor?,
        ktDelegate: KtPropertyDelegate
    ): IntermediateValue {
        val thisValue = createThisValueForDelegate(thisClass, ktDelegate)
        return BackingFieldLValue(
            context,
            ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset,
            irDelegateField.descriptor.type.toIrType(),
            irDelegateField,
            thisValue,
            null
        )
    }

    private fun createThisValueForDelegate(thisClass: ClassDescriptor?, ktDelegate: KtPropertyDelegate): IntermediateValue? =
        thisClass?.let {
            generateExpressionValue(it.thisAsReceiverParameter.type.toIrType()) {
                val thisAsReceiverParameter = thisClass.thisAsReceiverParameter
                IrGetValueImpl(
                    ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset,
                    thisAsReceiverParameter.type.toIrType(),
                    context.symbolTable.referenceValueParameter(thisAsReceiverParameter)
                )
            }
        }

    private fun createCallableReference(
        ktElement: KtElement,
        type: KotlinType,
        referencedDescriptor: CallableDescriptor,
        statementGenerator: StatementGenerator
    ): IrCallableReference =
        ReflectionReferencesGenerator(statementGenerator).generateCallableReference(
            ktElement, type,
            referencedDescriptor,
            null, IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
        )

    private fun createCallableReference(
        ktElement: KtElement,
        type: KotlinType,
        referencedDescriptor: VariableDescriptorWithAccessors,
        scopeOwner: IrSymbol
    ): IrCallableReference =
        createCallableReference(
            ktElement, type, referencedDescriptor,
            createBodyGenerator(scopeOwner).createStatementGenerator()
        )

    private fun createLocalDelegatedPropertyReference(
        ktElement: KtElement,
        type: KotlinType,
        variableDescriptor: VariableDescriptorWithAccessors,
        irDelegateSymbol: IrVariableSymbol,
        scopeOwner: IrSymbol
    ): IrLocalDelegatedPropertyReference =
        ReflectionReferencesGenerator(createBodyGenerator(scopeOwner).createStatementGenerator()).generateLocalDelegatedPropertyReference(
            ktElement.startOffsetSkippingComments, ktElement.endOffset,
            type, variableDescriptor, irDelegateSymbol,
            IrStatementOrigin.PROPERTY_REFERENCE_FOR_DELEGATE
        )

    fun generateLocalDelegatedProperty(
        ktProperty: KtProperty,
        ktDelegate: KtPropertyDelegate,
        variableDescriptor: VariableDescriptorWithAccessors,
        scopeOwnerSymbol: IrSymbol
    ): IrLocalDelegatedProperty {
        val kPropertyType = getKPropertyTypeForLocalDelegatedProperty(variableDescriptor)

        val irLocalDelegatedProperty = context.symbolTable.declareLocalDelegatedProperty(
            ktProperty.startOffsetSkippingComments, ktProperty.endOffset, IrDeclarationOrigin.DEFINED,
            variableDescriptor,
            variableDescriptor.type.toIrType()
        ).apply {
            delegate = generateDelegateVariableForLocalDelegatedProperty(ktDelegate, variableDescriptor, kPropertyType, scopeOwnerSymbol)
        }

        val irDelegate = irLocalDelegatedProperty.delegate

        val getterDescriptor = variableDescriptor.getter!!
        val delegateReceiverValue = createVariableValueForDelegate(irDelegate.symbol, ktDelegate)
        irLocalDelegatedProperty.getter =
            createLocalPropertyAccessor(getterDescriptor, ktDelegate) { irGetter ->
                generateDelegatedPropertyGetterBody(
                    irGetter, ktDelegate, getterDescriptor, delegateReceiverValue,
                    createLocalDelegatedPropertyReference(
                        ktDelegate, kPropertyType,
                        variableDescriptor, irDelegate.symbol,
                        irGetter.symbol
                    )
                )
            }

        if (variableDescriptor.isVar) {
            val setterDescriptor = variableDescriptor.setter!!
            irLocalDelegatedProperty.setter =
                createLocalPropertyAccessor(setterDescriptor, ktDelegate) { irSetter ->
                    generateDelegatedPropertySetterBody(
                        irSetter, ktDelegate, setterDescriptor, delegateReceiverValue,
                        createLocalDelegatedPropertyReference(
                            ktDelegate, kPropertyType,
                            variableDescriptor, irDelegate.symbol,
                            irSetter.symbol
                        )
                    )
                }
        }

        return irLocalDelegatedProperty
    }

    private fun generateDelegateVariableForLocalDelegatedProperty(
        ktDelegate: KtPropertyDelegate,
        variableDescriptor: VariableDescriptorWithAccessors,
        kPropertyType: KotlinType,
        scopeOwner: IrSymbol
    ): IrVariable {
        val delegateType = getDelegatedPropertyDelegateType(variableDescriptor, ktDelegate)
        val delegateDescriptor = createLocalPropertyDelegatedDescriptor(variableDescriptor, delegateType, kPropertyType)

        return context.symbolTable.declareVariable(
            ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset, IrDeclarationOrigin.DELEGATE,
            delegateDescriptor, delegateDescriptor.type.toIrType()
        ).also { irVariable ->
            irVariable.initializer = generateInitializerForLocalDelegatedPropertyDelegate(
                variableDescriptor, kPropertyType, ktDelegate,
                irVariable.symbol,
                scopeOwner
            )
        }
    }

    private fun getDelegatedPropertyDelegateType(
        delegatedPropertyDescriptor: VariableDescriptorWithAccessors,
        ktDelegate: KtPropertyDelegate
    ): KotlinType {
        val provideDelegateResolvedCall = get(BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL, delegatedPropertyDescriptor)
        return if (provideDelegateResolvedCall != null)
            provideDelegateResolvedCall.resultingDescriptor.returnType!!
        else
            getTypeInferredByFrontendOrFail(ktDelegate.expression!!)
    }

    private fun generateInitializerForLocalDelegatedPropertyDelegate(
        variableDescriptor: VariableDescriptorWithAccessors,
        kPropertyType: KotlinType,
        ktDelegate: KtPropertyDelegate,
        delegateSymbol: IrVariableSymbol,
        scopeOwner: IrSymbol
    ): IrExpression {
        val ktDelegateExpression = ktDelegate.expression!!
        val irDelegateInitializer = createBodyGenerator(scopeOwner).createStatementGenerator().generateExpression(ktDelegateExpression)

        val provideDelegateResolvedCall =
            get(BindingContext.PROVIDE_DELEGATE_RESOLVED_CALL, variableDescriptor) ?: return irDelegateInitializer

        val statementGenerator = createBodyGenerator(scopeOwner).createStatementGenerator()

        val provideDelegateCall = statementGenerator.pregenerateCall(provideDelegateResolvedCall).apply {
            setExplicitReceiverValue(OnceExpressionValue(irDelegateInitializer))
            irValueArgumentsByIndex[1] =
                createLocalDelegatedPropertyReference(ktDelegate, kPropertyType, variableDescriptor, delegateSymbol, scopeOwner)
        }

        return CallGenerator(statementGenerator).generateCall(
            ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset, provideDelegateCall
        )
    }

    private fun createVariableValueForDelegate(irDelegate: IrVariableSymbol, ktDelegate: KtPropertyDelegate) =
        VariableLValue(
            context,
            ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset,
            irDelegate,
            irDelegate.descriptor.type.toIrType()
        )

    private inline fun createLocalPropertyAccessor(
        getterDescriptor: VariableAccessorDescriptor,
        ktDelegate: KtPropertyDelegate,
        generateBody: (IrFunction) -> IrBody
    ) =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            ktDelegate.startOffsetSkippingComments, ktDelegate.endOffset,
            IrDeclarationOrigin.DELEGATED_PROPERTY_ACCESSOR,
            getterDescriptor
        ).buildWithScope { irAccessor ->
            FunctionGenerator(declarationGenerator).generateFunctionParameterDeclarationsAndReturnType(irAccessor, ktDelegate, null)
            irAccessor.body = generateBody(irAccessor)
        }

    private fun createLocalPropertyDelegatedDescriptor(
        variableDescriptor: VariableDescriptorWithAccessors,
        delegateType: KotlinType,
        kPropertyType: KotlinType
    ): IrLocalDelegatedPropertyDelegateDescriptor {
        return IrLocalDelegatedPropertyDelegateDescriptorImpl(variableDescriptor, delegateType, kPropertyType)
    }

    private fun getKPropertyTypeForLocalDelegatedProperty(variableDescriptor: VariableDescriptorWithAccessors) =
        context.reflectionTypes.getKPropertyType(Annotations.EMPTY, emptyList(), variableDescriptor.type, variableDescriptor.isVar)

    private fun createPropertyDelegateDescriptor(
        propertyDescriptor: PropertyDescriptor,
        delegateType: KotlinType,
        kPropertyType: KotlinType
    ): IrPropertyDelegateDescriptor =
        IrPropertyDelegateDescriptorImpl(propertyDescriptor, delegateType, kPropertyType)

    private fun generateDelegatedPropertyGetterBody(
        irGetter: IrFunction,
        ktDelegate: KtPropertyDelegate,
        getterDescriptor: VariableAccessorDescriptor,
        delegateReceiverValue: IntermediateValue,
        irPropertyReference: IrCallableReference
    ): IrBody =
        with(createBodyGenerator(irGetter.symbol)) {
            val startOffset = ktDelegate.startOffsetSkippingComments
            val endOffset = ktDelegate.endOffset
            irBlockBody(startOffset, endOffset) {
                val statementGenerator = createStatementGenerator()
                val conventionMethodResolvedCall = getOrFail(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor)
                val conventionMethodCall = statementGenerator.pregenerateCall(conventionMethodResolvedCall)
                conventionMethodCall.setExplicitReceiverValue(delegateReceiverValue)
                conventionMethodCall.irValueArgumentsByIndex[1] = irPropertyReference
                +irReturn(
                    CallGenerator(statementGenerator).generateCall(
                        startOffset,
                        endOffset,
                        conventionMethodCall
                    )
                )
            }
        }

    private fun generateDelegatedPropertySetterBody(
        irSetter: IrFunction,
        ktDelegate: KtPropertyDelegate,
        setterDescriptor: VariableAccessorDescriptor,
        delegateReceiverValue: IntermediateValue,
        irPropertyReference: IrCallableReference
    ): IrBody = with(createBodyGenerator(irSetter.symbol)) {
        val startOffset = ktDelegate.startOffsetSkippingComments
        val endOffset = ktDelegate.endOffset
        irBlockBody(startOffset, endOffset) {
            val statementGenerator = createStatementGenerator()
            val conventionMethodResolvedCall = getOrFail(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, setterDescriptor)
            val conventionMethodCall = statementGenerator.pregenerateCall(conventionMethodResolvedCall)
            conventionMethodCall.setExplicitReceiverValue(delegateReceiverValue)
            conventionMethodCall.irValueArgumentsByIndex[1] = irPropertyReference
            val irSetterParameter = irSetter.valueParameters[0]
            conventionMethodCall.irValueArgumentsByIndex[2] = irGet(irSetterParameter.type, irSetterParameter.symbol)
            +irReturn(CallGenerator(statementGenerator).generateCall(startOffset, endOffset, conventionMethodCall))
        }
    }
}