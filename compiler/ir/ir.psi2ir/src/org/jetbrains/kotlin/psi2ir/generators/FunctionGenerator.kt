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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi2ir.isConstructorDelegatingToSuper
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor

class FunctionGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {

    constructor(context: GeneratorContext) : this(DeclarationGenerator(context))

    fun generateFunctionDeclaration(ktFunction: KtNamedFunction): IrFunction =
        declareSimpleFunction(
            ktFunction,
            ktFunction.receiverTypeReference,
            IrDeclarationOrigin.DEFINED,
            getOrFail(BindingContext.FUNCTION, ktFunction)
        ) {
            ktFunction.bodyExpression?.let { generateFunctionBody(it) }
        }

    fun generateLambdaFunctionDeclaration(ktFunction: KtFunctionLiteral): IrSimpleFunction =
        declareSimpleFunction(
            ktFunction,
            null,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            getOrFail(BindingContext.FUNCTION, ktFunction)
        ) {
            generateLambdaBody(ktFunction)
        }

    fun generateFakeOverrideFunction(functionDescriptor: FunctionDescriptor, ktElement: KtPureElement): IrSimpleFunction? =
        functionDescriptor.takeIf { it.visibility != Visibilities.INVISIBLE_FAKE }
            ?.let {
                declareSimpleFunctionInner(it, ktElement, IrDeclarationOrigin.FAKE_OVERRIDE).buildWithScope { irFunction ->
                    generateFunctionParameterDeclarationsAndReturnType(irFunction, ktElement, null)
                }
            }

    private inline fun declareSimpleFunction(
        ktFunction: KtFunction,
        ktReceiver: KtElement?,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor,
        generateBody: BodyGenerator.() -> IrBody?
    ): IrSimpleFunction =
        declareSimpleFunctionInner(descriptor, ktFunction, origin).buildWithScope { irFunction ->
            generateFunctionParameterDeclarationsAndReturnType(irFunction, ktFunction, ktReceiver)
            irFunction.body = createBodyGenerator(irFunction.symbol).generateBody()
        }

    private fun declareSimpleFunctionInner(
        descriptor: FunctionDescriptor,
        ktElement: KtPureElement,
        origin: IrDeclarationOrigin
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            ktElement.getStartOffsetOfFunctionDeclarationKeywordOrNull() ?: ktElement.pureStartOffset,
            ktElement.pureEndOffset,
            origin,
            descriptor
        )

    fun generateFunctionParameterDeclarationsAndReturnType(
        irFunction: IrFunction,
        ktParameterOwner: KtPureElement?,
        ktReceiverParameterElement: KtElement?
    ) {
        declarationGenerator.generateScopedTypeParameterDeclarations(irFunction, irFunction.descriptor.propertyIfAccessor.typeParameters)
        irFunction.returnType = irFunction.descriptor.returnType!!.toIrType()
        generateValueParameterDeclarations(irFunction, ktParameterOwner, ktReceiverParameterElement)
    }

    fun generatePropertyAccessor(
        descriptor: PropertyAccessorDescriptor,
        ktProperty: KtProperty,
        ktAccessor: KtPropertyAccessor?
    ): IrSimpleFunction =
        declareSimpleFunctionInner(
            descriptor,
            ktAccessor ?: ktProperty,
            if (ktAccessor != null) IrDeclarationOrigin.DEFINED else IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
        ).buildWithScope { irAccessor ->
            declarationGenerator.generateScopedTypeParameterDeclarations(irAccessor, descriptor.correspondingProperty.typeParameters)
            irAccessor.returnType = irAccessor.descriptor.returnType!!.toIrType()
            generateValueParameterDeclarations(irAccessor, ktAccessor ?: ktProperty, ktProperty.receiverTypeReference)
            val ktBodyExpression = ktAccessor?.bodyExpression
            irAccessor.body =
                    if (ktBodyExpression != null)
                        createBodyGenerator(irAccessor.symbol).generateFunctionBody(ktBodyExpression)
                    else
                        generateDefaultAccessorBody(descriptor, irAccessor)
        }

    fun generateDefaultAccessorForPrimaryConstructorParameter(
        descriptor: PropertyAccessorDescriptor,
        ktParameter: KtParameter
    ): IrSimpleFunction =
        declareSimpleFunctionInner(descriptor, ktParameter, IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR).buildWithScope { irAccessor ->
            declarationGenerator.generateScopedTypeParameterDeclarations(irAccessor, descriptor.typeParameters)
            irAccessor.returnType = descriptor.returnType!!.toIrType()
            FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irAccessor)
            irAccessor.body = generateDefaultAccessorBody(descriptor, irAccessor)
        }

    private fun generateDefaultAccessorBody(
        accessor: PropertyAccessorDescriptor,
        irAccessor: IrSimpleFunction
    ) =
        if (accessor.modality == Modality.ABSTRACT)
            null
        else
            when (accessor) {
                is PropertyGetterDescriptor -> generateDefaultGetterBody(accessor, irAccessor)
                is PropertySetterDescriptor -> generateDefaultSetterBody(accessor, irAccessor)
                else -> throw AssertionError("Should be getter or setter: $accessor")
            }

    private fun generateDefaultGetterBody(
        getter: PropertyGetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = getter.correspondingProperty

        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(property, irAccessor)

        irBody.statements.add(
            IrReturnImpl(
                startOffset, endOffset, context.irBuiltIns.nothingType,
                irAccessor.symbol,
                IrGetFieldImpl(
                    startOffset, endOffset,
                    context.symbolTable.referenceField(property),
                    property.type.toIrType(),
                    receiver
                )
            )
        )
        return irBody
    }

    private fun generateDefaultSetterBody(
        setter: PropertySetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = setter.correspondingProperty

        val startOffset = irAccessor.startOffset
        val endOffset = irAccessor.endOffset
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(property, irAccessor)

        val irValueParameter = irAccessor.valueParameters.single()
        irBody.statements.add(
            IrSetFieldImpl(
                startOffset, endOffset,
                context.symbolTable.referenceField(property),
                receiver,
                IrGetValueImpl(startOffset, endOffset, irValueParameter.type, irValueParameter.symbol),
                context.irBuiltIns.unitType
            )
        )
        return irBody
    }

    private fun generateReceiverExpressionForDefaultPropertyAccessor(
        property: PropertyDescriptor,
        irAccessor: IrSimpleFunction
    ): IrExpression? {
        val containingDeclaration = property.containingDeclaration
        return when (containingDeclaration) {
            is ClassDescriptor -> {
                val thisAsReceiverParameter = containingDeclaration.thisAsReceiverParameter
                IrGetValueImpl(
                    irAccessor.startOffset, irAccessor.endOffset,
                    thisAsReceiverParameter.type.toIrType(),
                    context.symbolTable.referenceValue(thisAsReceiverParameter)
                )
            }
            else -> null
        }
    }

    fun generatePrimaryConstructor(
        primaryConstructorDescriptor: ClassConstructorDescriptor,
        ktClassOrObject: KtPureClassOrObject
    ): IrConstructor =
        declareConstructor(ktClassOrObject, ktClassOrObject.primaryConstructor ?: ktClassOrObject, primaryConstructorDescriptor) {
            if (
                primaryConstructorDescriptor.isExpect ||
                DescriptorUtils.isAnnotationClass(primaryConstructorDescriptor.constructedClass) ||
                primaryConstructorDescriptor.constructedClass.isExternal
            )
                null
            else
                generatePrimaryConstructorBody(ktClassOrObject)
        }

    fun generateSecondaryConstructor(ktConstructor: KtSecondaryConstructor): IrConstructor {
        val constructorDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor) as ClassConstructorDescriptor
        return declareConstructor(ktConstructor, ktConstructor, constructorDescriptor) {
            when {
                constructorDescriptor.constructedClass.isExternal ->
                    null

                ktConstructor.isConstructorDelegatingToSuper(context.bindingContext) ->
                    generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor)

                else -> generateSecondaryConstructorBody(ktConstructor)
            }
        }
    }


    private inline fun declareConstructor(
        ktConstructorElement: KtPureElement,
        ktParametersElement: KtPureElement,
        constructorDescriptor: ClassConstructorDescriptor,
        generateBody: BodyGenerator.() -> IrBody?
    ): IrConstructor =
        context.symbolTable.declareConstructor(
            ktConstructorElement.getStartOffsetOfConstructorDeclarationKeywordOrNull() ?: ktConstructorElement.pureStartOffset,
            ktConstructorElement.pureEndOffset,
            IrDeclarationOrigin.DEFINED,
            constructorDescriptor
        ).buildWithScope { irConstructor ->
            generateValueParameterDeclarations(irConstructor, ktParametersElement, null)
            irConstructor.body = createBodyGenerator(irConstructor.symbol).generateBody()
            irConstructor.returnType = constructorDescriptor.returnType.toIrType()
        }

    fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
        declarationGenerator.generateGlobalTypeParametersDeclarations(irFunction, irFunction.descriptor.typeParameters)
        generateValueParameterDeclarations(irFunction, null, null, withDefaultValues = false)
    }

    private fun generateValueParameterDeclarations(
        irFunction: IrFunction,
        ktParameterOwner: KtPureElement?,
        ktReceiverParameterElement: KtPureElement?,
        withDefaultValues: Boolean = true
    ) {
        val functionDescriptor = irFunction.descriptor

        irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, ktParameterOwner, irFunction)
        }

        irFunction.extensionReceiverParameter = functionDescriptor.extensionReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, ktReceiverParameterElement ?: ktParameterOwner, irFunction)
        }

        val bodyGenerator = createBodyGenerator(irFunction.symbol)
        functionDescriptor.valueParameters.mapTo(irFunction.valueParameters) { valueParameterDescriptor ->
            val ktParameter = DescriptorToSourceUtils.getSourceFromDescriptor(valueParameterDescriptor) as? KtParameter
            generateValueParameterDeclaration(valueParameterDescriptor, ktParameter, bodyGenerator, withDefaultValues, irFunction)
        }
    }

    private fun generateValueParameterDeclaration(
        valueParameterDescriptor: ValueParameterDescriptor,
        ktParameter: KtParameter?,
        bodyGenerator: BodyGenerator,
        withDefaultValues: Boolean,
        irOwnerElement: IrElement
    ): IrValueParameter =
        declareParameter(valueParameterDescriptor, ktParameter, irOwnerElement).also { irValueParameter ->
            if (withDefaultValues) {
                irValueParameter.defaultValue = ktParameter?.defaultValue?.let {
                    bodyGenerator.generateExpressionBody(it)
                }
            }
        }

    private fun generateReceiverParameterDeclaration(
        receiverParameterDescriptor: ReceiverParameterDescriptor,
        ktElement: KtPureElement?,
        irOwnerElement: IrElement
    ): IrValueParameter =
        declareParameter(receiverParameterDescriptor, ktElement, irOwnerElement)

    private fun declareParameter(descriptor: ParameterDescriptor, ktElement: KtPureElement?, irOwnerElement: IrElement) =
        context.symbolTable.declareValueParameter(
            ktElement?.pureStartOffset ?: irOwnerElement.startOffset,
            ktElement?.pureEndOffset ?: irOwnerElement.endOffset,
            IrDeclarationOrigin.DEFINED,
            descriptor, descriptor.type.toIrType(),
            (descriptor as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        )
}