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
                irFunction.body = createBodyGenerator(descriptor).generateBody()
            }

    fun generateFunctionParameterDeclarations(
            irFunction: IrFunction,
            ktParameterOwner: KtElement,
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
                            createBodyGenerator(descriptor).generateFunctionBody(ktBodyExpression)
                        else
                            generateDefaultAccessorBody(ktProperty, descriptor)
            }

    fun generateDefaultAccessorForPrimaryConstructorParameter(
            descriptor: PropertyAccessorDescriptor,
            ktParameter: KtParameter,
            isGetter: Boolean
    ): IrFunction =
            context.symbolTable.declareSimpleFunction(
                    ktParameter.startOffsetOrUndefined,
                    ktParameter.endOffsetOrUndefined,
                    IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR,
                    descriptor
            ).buildWithScope { irAccessor ->
                val accessorDescriptor = irAccessor.descriptor
                declarationGenerator.generateTypeParameterDeclarations(irAccessor, accessorDescriptor.typeParameters)
                FunctionGenerator(declarationGenerator).generateSyntheticFunctionParameterDeclarations(irAccessor)
                irAccessor.body =
                        if (isGetter) generateDefaultGetterBody(ktParameter, descriptor as PropertyGetterDescriptor)
                        else generateDefaultSetterBody(ktParameter, descriptor as PropertySetterDescriptor)
            }

    private fun generateDefaultAccessorBody(ktProperty: KtElement, accessor: PropertyAccessorDescriptor) =
            when (accessor) {
                is PropertyGetterDescriptor -> generateDefaultGetterBody(ktProperty, accessor)
                is PropertySetterDescriptor -> generateDefaultSetterBody(ktProperty, accessor)
                else -> throw AssertionError("Should be getter or setter: $accessor")
            }

    private fun generateDefaultGetterBody(ktProperty: KtElement, getter: PropertyGetterDescriptor): IrBlockBody {
        val property = getter.correspondingProperty

        val irBody = IrBlockBodyImpl(ktProperty.startOffset, ktProperty.endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(ktProperty, property)

        irBody.statements.add(IrReturnImpl(ktProperty.startOffset, ktProperty.endOffset, context.builtIns.nothingType, getter,
                                           IrGetFieldImpl(ktProperty.startOffset, ktProperty.endOffset, property, receiver)))
        return irBody
    }

    private fun generateDefaultSetterBody(ktProperty: KtElement, setter: PropertySetterDescriptor): IrBlockBody {
        val property = setter.correspondingProperty

        val irBody = IrBlockBodyImpl(ktProperty.startOffset, ktProperty.endOffset)

        val receiver = generateReceiverExpressionForDefaultPropertyAccessor(ktProperty, property)

        val setterParameter = setter.valueParameters.single()
        irBody.statements.add(IrSetFieldImpl(ktProperty.startOffset, ktProperty.endOffset, property, receiver,
                                             IrGetValueImpl(ktProperty.startOffset, ktProperty.endOffset, setterParameter)))
        return irBody
    }

    private fun generateReceiverExpressionForDefaultPropertyAccessor(ktProperty: KtElement, property: PropertyDescriptor): IrExpression? {
        val containingDeclaration = property.containingDeclaration
        val receiver = when (containingDeclaration) {
            is ClassDescriptor ->
                IrGetValueImpl(ktProperty.startOffset, ktProperty.endOffset, containingDeclaration.thisAsReceiverParameter)
            else -> null
        }
        return receiver
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
                generateFunctionParameterDeclarations(irConstructor, ktParametersElement, null)
                irConstructor.body = createBodyGenerator(constructorDescriptor).generateBody()
            }

    fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
        declarationGenerator.generateTypeParameterDeclarations(irFunction, irFunction.descriptor.typeParameters)
        generateValueParameterDeclarations(irFunction, null, null)
    }

    fun generateValueParameterDeclarations(
            irFunction: IrFunction,
            ktParameterOwner: KtElement?,
            ktReceiverParameterElement: KtElement?
    ) {
        val functionDescriptor = irFunction.descriptor

        irFunction.dispatchReceiverParameter = functionDescriptor.dispatchReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, ktParameterOwner)
        }

        irFunction.extensionReceiverParameter = functionDescriptor.extensionReceiverParameter?.let {
            generateReceiverParameterDeclaration(it, ktReceiverParameterElement ?: ktParameterOwner)
        }

        val bodyGenerator = createBodyGenerator(functionDescriptor)
        functionDescriptor.valueParameters.mapTo(irFunction.valueParameters) { valueParameterDescriptor ->
            val ktParameter = DescriptorToSourceUtils.getSourceFromDescriptor(valueParameterDescriptor) as? KtParameter
            generateValueParameterDeclaration(valueParameterDescriptor, ktParameter, bodyGenerator)
        }
    }

    private fun generateValueParameterDeclaration(
            valueParameterDescriptor: ValueParameterDescriptor,
            ktParameter: KtParameter?,
            bodyGenerator: BodyGenerator
    ): IrValueParameter =
            context.symbolTable.declareValueParameter(
                    ktParameter.startOffsetOrUndefined,
                    ktParameter.endOffsetOrUndefined,
                    IrDeclarationOrigin.DEFINED,
                    valueParameterDescriptor
            ).also {
                it.defaultValue = ktParameter?.defaultValue?.let {
                    bodyGenerator.generateExpressionBody(it)
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