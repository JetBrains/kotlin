/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.types.impl.IrUninitializedType
import org.jetbrains.kotlin.ir.util.declareSimpleFunctionWithOverrides
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi2ir.isConstructorDelegatingToSuper
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyExternal
import org.jetbrains.kotlin.resolve.descriptorUtil.propertyIfAccessor
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FunctionGenerator(declarationGenerator: DeclarationGenerator) : DeclarationGeneratorExtension(declarationGenerator) {

    constructor(context: GeneratorContext) : this(DeclarationGenerator(context))

    fun generateFunctionDeclaration(ktFunction: KtNamedFunction): IrSimpleFunction =
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
        functionDescriptor.takeIf { it.visibility != DescriptorVisibilities.INVISIBLE_FAKE }
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
        ktProperty: KtVariableDeclaration,
        ktAccessor: KtPropertyAccessor?
    ): IrSimpleFunction =
        declareSimpleFunctionInner(
            descriptor,
            ktAccessor ?: ktProperty,
            if (ktAccessor != null && ktAccessor.hasBody()) IrDeclarationOrigin.DEFINED else IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR
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
        ktElement: KtElement
    ): IrSimpleFunction =
        declareSimpleFunctionInner(descriptor, ktElement, IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR).buildWithScope { irAccessor ->
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
        val irBody = context.irFactory.createBlockBody(startOffset, endOffset)

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
        val irBody = context.irFactory.createBlockBody(startOffset, endOffset)

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
                primaryConstructorDescriptor.constructedClass.isEffectivelyExternal()
            )
                null
            else
                generatePrimaryConstructorBody(ktClassOrObject)
        }

    fun generateSecondaryConstructor(ktConstructor: KtSecondaryConstructor): IrConstructor {
        val constructorDescriptor = getOrFail(BindingContext.CONSTRUCTOR, ktConstructor) as ClassConstructorDescriptor
        return declareConstructor(ktConstructor, ktConstructor, constructorDescriptor) {
            when {
                constructorDescriptor.constructedClass.isEffectivelyExternal() ->
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
    ): IrConstructor {
        val startOffset = ktConstructorElement.getStartOffsetOfConstructorDeclarationKeywordOrNull() ?: ktConstructorElement.pureStartOffset
        val endOffset = ktConstructorElement.pureEndOffset
        val origin = IrDeclarationOrigin.DEFINED
        return context.symbolTable.declareConstructor(constructorDescriptor) {
            with(constructorDescriptor) {
                context.irFactory.createConstructor(
                    startOffset, endOffset, origin, it, context.symbolTable.nameProvider.nameForDeclaration(this),
                    visibility, IrUninitializedType, isInline, isEffectivelyExternal(), isPrimary, isExpect
                )
            }.apply {
                metadata = DescriptorMetadataSource.Function(it.descriptor)
            }
        }.buildWithScope { irConstructor ->
            generateValueParameterDeclarations(irConstructor, ktParametersElement, null)
            irConstructor.body = createBodyGenerator(irConstructor.symbol).generateBody()
            irConstructor.returnType = constructorDescriptor.returnType.toIrType()
        }
    }

    fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
        val descriptor = irFunction.descriptor
        val typeParameters =
            if (descriptor is PropertyAccessorDescriptor)
                descriptor.correspondingProperty.typeParameters
            else
                descriptor.typeParameters
        declarationGenerator.generateScopedTypeParameterDeclarations(irFunction, typeParameters)
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

        // Declare all the value parameters up first.
        irFunction.valueParameters += functionDescriptor.valueParameters.map { valueParameterDescriptor ->
            val ktParameter = DescriptorToSourceUtils.getSourceFromDescriptor(valueParameterDescriptor) as? KtParameter
            declareParameter(valueParameterDescriptor, ktParameter, irFunction)
        }
        // Only after value parameters have been declared, generate default values. This ensures
        // that forward references to other parameters works in default value lambdas. For example:
        //
        // fun f(f1: () -> String = { f2() },
        //       f2: () -> String) = f1()
        if (withDefaultValues) {
            irFunction.valueParameters.forEachIndexed { index, irValueParameter ->
                val valueParameterDescriptor = functionDescriptor.valueParameters[index]
                val ktParameter = DescriptorToSourceUtils.getSourceFromDescriptor(valueParameterDescriptor) as? KtParameter
                irValueParameter.defaultValue = ktParameter?.defaultValue?.let { defaultValue ->
                    val inAnnotation =
                        valueParameterDescriptor.containingDeclaration.safeAs<ConstructorDescriptor>()?.isAnnotationConstructor() ?: false
                    if (inAnnotation) {
                        generateDefaultAnnotationParameterValue(defaultValue, valueParameterDescriptor)
                    } else
                        bodyGenerator.generateExpressionBody(defaultValue)
                }
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

    private fun generateDefaultAnnotationParameterValue(
        valueExpression: KtExpression,
        valueParameterDescriptor: ValueParameterDescriptor
    ): IrExpressionBody {
        val constantDefaultValue =
            ConstantExpressionEvaluator.getConstant(valueExpression, context.bindingContext)?.toConstantValue(valueParameterDescriptor.type)
                ?: error("Constant value expected for default parameter value in annotation, got $valueExpression")
        return context.irFactory.createExpressionBody(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.constantValueGenerator.generateConstantValueAsExpression(
                UNDEFINED_OFFSET, UNDEFINED_OFFSET, constantDefaultValue, valueParameterDescriptor.varargElementType
            )
        )
    }
}
