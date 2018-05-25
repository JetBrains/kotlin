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
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.endOffsetOrUndefined
import org.jetbrains.kotlin.psi2ir.isConstructorDelegatingToSuper
import org.jetbrains.kotlin.psi2ir.startOffsetOrUndefined
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils

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

    fun generateFakeOverrideFunction(functionDescriptor: FunctionDescriptor, ktElement: KtElement): IrSimpleFunction =
        declareSimpleFunctionInner(functionDescriptor, ktElement, IrDeclarationOrigin.FAKE_OVERRIDE).buildWithScope { irFunction ->
            generateFunctionParameterDeclarationsAndReturnType(irFunction, ktElement, null)
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
        ktElement: KtElement,
        origin: IrDeclarationOrigin
    ): IrSimpleFunction =
        context.symbolTable.declareSimpleFunctionWithOverrides(
            ktElement.startOffset, ktElement.endOffset, origin,
            descriptor
        )

    fun generateFunctionParameterDeclarationsAndReturnType(
        irFunction: IrFunction,
        ktParameterOwner: KtElement?,
        ktReceiverParameterElement: KtElement?
    ) {
        declarationGenerator.generateScopedTypeParameterDeclarations(irFunction, irFunction.descriptor.typeParameters)
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
            generateFunctionParameterDeclarationsAndReturnType(irAccessor, ktAccessor ?: ktProperty, ktProperty.receiverTypeReference)
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
    ): IrSimpleFunction =
        declareSimpleFunctionInner(descriptor, ktParameter, IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR).buildWithScope { irAccessor ->
            declarationGenerator.generateScopedTypeParameterDeclarations(irAccessor, descriptor.typeParameters)
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

        val startOffset = ktProperty.startOffset
        val endOffset = ktProperty.endOffset
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(ktProperty, property)

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
        ktProperty: KtElement,
        setter: PropertySetterDescriptor,
        irAccessor: IrSimpleFunction
    ): IrBlockBody {
        val property = setter.correspondingProperty

        val startOffset = ktProperty.startOffset
        val endOffset = ktProperty.endOffset
        val irBody = IrBlockBodyImpl(startOffset, endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(ktProperty, property)

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

    private fun generateReceiverExpressionForDefaultPropertyAccessor(ktProperty: KtElement, property: PropertyDescriptor): IrExpression? {
        val containingDeclaration = property.containingDeclaration
        return when (containingDeclaration) {
            is ClassDescriptor -> {
                val thisAsReceiverParameter = containingDeclaration.thisAsReceiverParameter
                IrGetValueImpl(
                    ktProperty.startOffset, ktProperty.endOffset,
                    thisAsReceiverParameter.type.toIrType(),
                    context.symbolTable.referenceValue(thisAsReceiverParameter)
                )
            }
            else -> null
        }
    }

    fun generatePrimaryConstructor(
        primaryConstructorDescriptor: ClassConstructorDescriptor,
        ktClassOrObject: KtClassOrObject
    ): IrConstructor =
        declareConstructor(ktClassOrObject, ktClassOrObject.primaryConstructor ?: ktClassOrObject, primaryConstructorDescriptor) {
            if (
                primaryConstructorDescriptor.isExpect ||
                DescriptorUtils.isAnnotationClass(primaryConstructorDescriptor.constructedClass)
            )
                null
            else
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
        generateBody: BodyGenerator.() -> IrBody?
    ): IrConstructor =
        context.symbolTable.declareConstructor(
            ktConstructorElement.startOffset, ktConstructorElement.endOffset, IrDeclarationOrigin.DEFINED,
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
        declareParameter(valueParameterDescriptor, ktParameter).also {
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
        declareParameter(receiverParameterDescriptor, ktElement)

    private fun declareParameter(descriptor: ParameterDescriptor, ktElement: KtElement?) =
        context.symbolTable.declareValueParameter(
            ktElement.startOffsetOrUndefined, ktElement.endOffsetOrUndefined,
            IrDeclarationOrigin.DEFINED,
            descriptor, descriptor.type.toIrType()
        )
}