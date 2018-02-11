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
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.isConstructorDelegatingToSuper
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

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

    fun generateLambdaFunctionDeclaration(ktFunction: KtFunctionLiteral): IrFunction =
        declareSimpleFunction(
            ktFunction,
            null,
            IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA,
            getOrFail(BindingContext.FUNCTION, ktFunction)
        ) {
            generateLambdaBody(ktFunction)
        }

    fun generateFakeOverrideFunction(functionDescriptor: FunctionDescriptor, ktElement: KtElement): IrFunction =
        context.symbolTable.declareSimpleFunction(
            ktElement.startOffsetOrUndefined, ktElement.endOffsetOrUndefined,
            IrDeclarationOrigin.FAKE_OVERRIDE,
            functionDescriptor
        ).buildWithScope { irFunction ->
            generateFunctionParameterDeclarations(irFunction, ktElement, null)
        }

    private inline fun declareSimpleFunction(
        ktFunction: KtFunction,
        ktReceiver: KtElement?,
        origin: IrDeclarationOrigin,
        descriptor: FunctionDescriptor,
        generateBody: BodyGenerator.() -> IrBody?
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunction(
            ktFunction.startOffset, ktFunction.endOffset, origin, descriptor
        ).buildWithScope { irFunction ->
            generateFunctionParameterDeclarations(irFunction, ktFunction, ktReceiver)
            irFunction.body = createBodyGenerator(irFunction.symbol).generateBody()
        }

    fun generateFunctionParameterDeclarations(
        irFunction: IrFunction,
        ktParameterOwner: KtElement?,
        ktReceiverParameterElement: KtElement?
    ) {
        declarationGenerator.generateTypeParameterDeclarations(irFunction, irFunction.descriptor.typeParameters)
        generateValueParameterDeclarations(irFunction, ktParameterOwner, ktReceiverParameterElement)
    }

    fun generatePropertyAccessor(
        descriptor: PropertyAccessorDescriptor,
        ktProperty: KtProperty,
        ktAccessor: KtPropertyAccessor?
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunction(
            ktAccessor?.startOffset ?: ktProperty.startOffset,
            ktAccessor?.endOffset ?: ktProperty.endOffset,
            if (ktAccessor != null) IrDeclarationOrigin.DEFINED else IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
            descriptor
        ).buildWithScope { irAccessor ->
            generateFunctionParameterDeclarations(irAccessor, ktAccessor ?: ktProperty, ktProperty.receiverTypeReference)
            val ktBodyExpression = ktAccessor?.bodyExpression
            irAccessor.body =
                    if (ktBodyExpression != null)
                        createBodyGenerator(irAccessor.symbol).generateFunctionBody(ktBodyExpression)
                    else
                        generateDefaultAccessorBody(ktProperty, descriptor, irAccessor)
        }

    fun generateDefaultAccessorForPrimaryConstructorParameter(
        descriptor: PropertyAccessorDescriptor,
        ktParameter: KtParameter
    ): IrFunction =
        context.symbolTable.declareSimpleFunction(
            ktParameter.startOffsetOrUndefined,
            ktParameter.endOffsetOrUndefined,
            IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
            descriptor
        ).buildWithScope { irAccessor ->
            declarationGenerator.generateTypeParameterDeclarations(irAccessor, descriptor.typeParameters)
            FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irAccessor)
            irAccessor.body = generateDefaultAccessorBody(ktParameter, descriptor, irAccessor)
        }

    private fun generateDefaultAccessorBody(ktProperty: KtElement, accessor: PropertyAccessorDescriptor, irAccessor: IrSimpleFunction) =
        if (accessor.modality == Modality.ABSTRACT)
            null
        else
            when (accessor) {
                is PropertyGetterDescriptor -> generateDefaultGetterBody(ktProperty, accessor, irAccessor)
                is PropertySetterDescriptor -> generateDefaultSetterBody(ktProperty, accessor, irAccessor)
                else -> throw AssertionError("Should be getter or setter: $accessor")
            }

    private fun generateDefaultGetterBody(
        ktProperty: KtElement,
        getter: PropertyGetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = getter.correspondingProperty

        val irBody = IrBlockBodyImpl(ktProperty.startOffset, ktProperty.endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(ktProperty, property)

        irBody.statements.add(
            IrReturnImpl(
                ktProperty.startOffset, ktProperty.endOffset, context.builtIns.nothingType,
                irAccessor.symbol,
                IrGetFieldImpl(
                    ktProperty.startOffset, ktProperty.endOffset,
                    context.symbolTable.referenceField(property),
                    receiver
                )
            )
        )
        return irBody
    }

    private fun generateDefaultSetterBody(
        ktProperty: KtElement,
        setter: PropertySetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = setter.correspondingProperty

        val irBody = IrBlockBodyImpl(ktProperty.startOffset, ktProperty.endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(ktProperty, property)

        val setterParameter = irAccessor.valueParameters.single().symbol
        irBody.statements.add(
            IrSetFieldImpl(
                ktProperty.startOffset, ktProperty.endOffset,
                context.symbolTable.referenceField(property),
                receiver,
                IrGetValueImpl(ktProperty.startOffset, ktProperty.endOffset, setterParameter)
            )
        )
        return irBody
    }

    private fun generateReceiverExpressionForDefaultPropertyAccessor(ktProperty: KtElement, property: PropertyDescriptor): IrExpression? {
        val containingDeclaration = property.containingDeclaration
        return when (containingDeclaration) {
            is ClassDescriptor ->
                IrGetValueImpl(
                    ktProperty.startOffset, ktProperty.endOffset,
                    context.symbolTable.referenceValue(containingDeclaration.thisAsReceiverParameter)
                )
            else -> null
        }
    }

    fun generatePrimaryConstructor(
        primaryConstructorDescriptor: ClassConstructorDescriptor,
        ktClassOrObject: KtClassOrObject
    ): IrConstructor =
        declareConstructor(ktClassOrObject, ktClassOrObject.primaryConstructor ?: ktClassOrObject, primaryConstructorDescriptor) {
            generatePrimaryConstructorBody(ktClassOrObject)
        }

    fun generateSecondaryConstructor(ktConstructor: KtSecondaryConstructor): IrConstructor =
        declareConstructor(
            ktConstructor, ktConstructor,
            getOrFail(BindingContext.CONSTRUCTOR, ktConstructor) as ClassConstructorDescriptor
        ) {
            if (ktConstructor.isConstructorDelegatingToSuper(context.bindingContext))
                generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor)
            else
                generateSecondaryConstructorBody(ktConstructor)
        }


    private inline fun declareConstructor(
        ktConstructorElement: KtElement,
        ktParametersElement: KtElement,
        constructorDescriptor: ClassConstructorDescriptor,
        generateBody: BodyGenerator.() -> IrBody
    ): IrConstructor =
        context.symbolTable.declareConstructor(
            ktConstructorElement.startOffset, ktConstructorElement.endOffset, IrDeclarationOrigin.DEFINED, constructorDescriptor
        ).buildWithScope { irConstructor ->
            generateValueParameterDeclarations(irConstructor, ktParametersElement, null)
            irConstructor.body = createBodyGenerator(irConstructor.symbol).generateBody()
        }

    fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
        declarationGenerator.generateTypeParameterDeclarations(irFunction, irFunction.descriptor.typeParameters)
        generateValueParameterDeclarations(irFunction, null, null, withDefaultValues = false)
    }

    private fun generateValueParameterDeclarations(
        irFunction: IrFunction,
        ktParameterOwner: KtElement?,
        ktReceiverParameterElement: KtElement?,
        withDefaultValues: Boolean = true
    ) {
        val functionDescriptor = irFunction.descriptor

        irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, ktParameterOwner)
        }

        irFunction.extensionReceiverParameter = functionDescriptor.extensionReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, ktReceiverParameterElement ?: ktParameterOwner)
        }

        val bodyGenerator = createBodyGenerator(irFunction.symbol)
        functionDescriptor.valueParameters.mapTo(irFunction.valueParameters) { valueParameterDescriptor ->
            val ktParameter = DescriptorToSourceUtils.getSourceFromDescriptor(valueParameterDescriptor) as? KtParameter
            generateValueParameterDeclaration(valueParameterDescriptor, ktParameter, bodyGenerator, withDefaultValues)
        }
    }

    private fun generateValueParameterDeclaration(
        valueParameterDescriptor: ValueParameterDescriptor,
        ktParameter: KtParameter?,
        bodyGenerator: BodyGenerator,
        withDefaultValues: Boolean
    ): IrValueParameter =
        context.symbolTable.declareValueParameter(
            ktParameter.startOffsetOrUndefined,
            ktParameter.endOffsetOrUndefined,
            IrDeclarationOrigin.DEFINED,
            valueParameterDescriptor
        ).also {
            if (withDefaultValues) {
                it.defaultValue = ktParameter?.defaultValue?.let {
                    bodyGenerator.generateExpressionBody(it)
                }
            }
        }

    private fun generateReceiverParameterDeclaration(
        receiverParameterDescriptor: ReceiverParameterDescriptor,
        ktElement: KtElement?
    ): IrValueParameter =
        context.symbolTable.declareValueParameter(
            ktElement.startOffsetOrUndefined,
            ktElement.endOffsetOrUndefined,
            IrDeclarationOrigin.DEFINED,
            receiverParameterDescriptor
        )

}