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

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
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

    fun generateFunctionDeclaration(ktFunction: KtNamedFunction): IrFunction {
        val functionDescriptor = getOrFail(BindingContext.FUNCTION, ktFunction)
        val irFunction = IrFunctionImpl(ktFunction.startOffset, ktFunction.endOffset, IrDeclarationOrigin.DEFINED, functionDescriptor)
        generateFunctionParameterDeclarations(irFunction, ktFunction, ktFunction.receiverTypeReference)
        irFunction.body = ktFunction.bodyExpression?.let { createBodyGenerator(functionDescriptor).generateFunctionBody(it) }
        return irFunction
    }

    fun generateLambdaFunctionDeclaration(ktFunction: KtFunctionLiteral): IrFunction {
        val lambdaDescriptor = getOrFail(BindingContext.FUNCTION, ktFunction)
        val irLambdaFunction = IrFunctionImpl(ktFunction.startOffset, ktFunction.endOffset, IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA, lambdaDescriptor)
        generateFunctionParameterDeclarations(irLambdaFunction, ktFunction, null)
        irLambdaFunction.body = createBodyGenerator(lambdaDescriptor).generateLambdaBody(ktFunction)
        return irLambdaFunction
    }

    fun generateFunctionParameterDeclarations(
            irFunction: IrFunction,
            ktParameterOwner: KtElement,
            ktReceiverParameterElement: KtElement?
    ) {
        declarationGenerator.generateTypeParameterDeclarations(irFunction, irFunction.descriptor.typeParameters)
        generateValueParameterDeclarations(irFunction, ktParameterOwner, ktReceiverParameterElement)
    }

    fun generatePrimaryConstructor(
            primaryConstructorDescriptor: ClassConstructorDescriptor,
            ktClassOrObject: KtClassOrObject
    ): IrConstructor {
        val irPrimaryConstructor = IrConstructorImpl(
                ktClassOrObject.startOffset, ktClassOrObject.endOffset,
                IrDeclarationOrigin.DEFINED,
                primaryConstructorDescriptor
        )

        generateFunctionParameterDeclarations(
                irPrimaryConstructor,
                ktClassOrObject.primaryConstructor ?: ktClassOrObject,
                null
        )

        irPrimaryConstructor.body = BodyGenerator(primaryConstructorDescriptor, context).generatePrimaryConstructorBody(ktClassOrObject)

        return irPrimaryConstructor
    }

    fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
        declarationGenerator.generateTypeParameterDeclarations(irFunction, irFunction.descriptor.typeParameters)
        generateValueParameterDeclarations(irFunction, null, null)
    }

    private fun generateValueParameterDeclarations(
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
            IrValueParameterImpl(
                    ktParameter.startOffsetOrUndefined,
                    ktParameter.endOffsetOrUndefined,
                    IrDeclarationOrigin.DEFINED,
                    valueParameterDescriptor,
                    ktParameter?.defaultValue?.let {
                        bodyGenerator.generateExpressionBody(it)
                    }
            )

    private fun generateReceiverParameterDeclaration(
            receiverParameterDescriptor: ReceiverParameterDescriptor,
            ktElement: KtElement?
    ): IrValueParameter =
            IrValueParameterImpl(
                    ktElement.startOffsetOrUndefined,
                    ktElement.endOffsetOrUndefined,
                    IrDeclarationOrigin.DEFINED,
                    receiverParameterDescriptor
            )

    fun generateSecondaryConstructor(ktConstructor: KtSecondaryConstructor): IrFunction {
        val constructorDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor) as ClassConstructorDescriptor

        val irConstructor = IrConstructorImpl(ktConstructor.startOffset, ktConstructor.endOffset, IrDeclarationOrigin.DEFINED,
                                              constructorDescriptor)

        generateFunctionParameterDeclarations(irConstructor, ktConstructor, null)

        irConstructor.body = createBodyGenerator(constructorDescriptor).run {
            if (ktConstructor.isConstructorDelegatingToSuper(context.bindingContext))
                generateSecondaryConstructorBodyWithNestedInitializers(ktConstructor)
            else
                generateSecondaryConstructorBody(ktConstructor)
        }

        return irConstructor
    }



}