/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.transformations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi2ir.generators.ConstantValueGenerator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

fun generateAnnotationsForDeclarations(context: GeneratorContext, irElement: IrElement) {
    val annotationGenerator = AnnotationGenerator(context.moduleDescriptor, context.symbolTable)
    irElement.acceptVoid(annotationGenerator)
}

class AnnotationGenerator(
    moduleDescriptor: ModuleDescriptor,
    private val symbolTable: SymbolTable
) : IrElementVisitorVoid {

    constructor(context: GeneratorContext) : this(context.moduleDescriptor, context.symbolTable)

    private val scopedTypeParameterResolver = ScopedTypeParametersResolver()
    private val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable, this, scopedTypeParameterResolver)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        if (declaration is IrTypeParametersContainer) {
            scopedTypeParameterResolver.enterTypeParameterScope(declaration)
        }
        generateAnnotationsForDeclaration(declaration)
        visitElement(declaration)
        if (declaration is IrTypeParametersContainer) {
            scopedTypeParameterResolver.leaveTypeParameterScope()
        }
    }

    override fun visitValueParameter(declaration: IrValueParameter) {
        super.visitValueParameter(declaration)

        val descriptor = declaration.descriptor
        val containingDeclaration = descriptor.containingDeclaration

        if (containingDeclaration is PropertySetterDescriptor) {
            containingDeclaration.correspondingProperty.annotations.getUseSiteTargetedAnnotations()
                .filter { it.target == AnnotationUseSiteTarget.SETTER_PARAMETER }
                .generateAnnotationConstructorCalls(declaration)
        }

        descriptor.type.annotations.getAllAnnotations()
            .filter { it.target == AnnotationUseSiteTarget.RECEIVER }
            .generateAnnotationConstructorCalls(declaration)
    }

    private fun generateAnnotationsForDeclaration(declaration: IrDeclaration) {
        declaration.descriptor.annotations.getAllAnnotations()
            .filter { isAnnotationTargetMatchingDeclaration(it.target, declaration) }
            .generateAnnotationConstructorCalls(declaration)
    }

    private fun List<AnnotationWithTarget>.generateAnnotationConstructorCalls(declaration: IrDeclaration) {
        mapTo(declaration.annotations) {
            generateAnnotationConstructorCall(it.annotation)
        }
    }

    fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor): IrCall {
        val annotationType = annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor as? ClassDescriptor
                ?: throw AssertionError("No declaration descriptor for annotation $annotationDescriptor")
        assert(DescriptorUtils.isAnnotationClass(annotationClassDescriptor)) {
            "Annotation class expected: $annotationClassDescriptor"
        }

        val primaryConstructorDescriptor =
            annotationClassDescriptor.unsubstitutedPrimaryConstructor
                    ?: annotationClassDescriptor.constructors.singleOrNull()
                    ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")

        val primaryConstructorSymbol = symbolTable.referenceConstructor(primaryConstructorDescriptor)

        val psi = annotationDescriptor.source.safeAs<PsiSourceElement>()?.psi
        val startOffset = psi?.startOffset ?: UNDEFINED_OFFSET
        val endOffset = psi?.startOffset ?: UNDEFINED_OFFSET

        val irCall = IrCallImpl(
            startOffset, endOffset, annotationType,
            primaryConstructorSymbol, primaryConstructorDescriptor,
            typeArgumentsCount = 0
        )

        for (valueParameter in primaryConstructorDescriptor.valueParameters) {
            val argumentIndex = valueParameter.index
            val argumentValue = annotationDescriptor.allValueArguments[valueParameter.name] ?: continue
            val irArgument =
                constantValueGenerator.generateConstantValueAsExpression(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    argumentValue,
                    valueParameter.varargElementType
                )
            irCall.putValueArgument(argumentIndex, irArgument)
        }

        return irCall
    }

    private fun isAnnotationTargetMatchingDeclaration(target: AnnotationUseSiteTarget?, element: IrElement): Boolean =
        when (element) {
            is IrProperty ->
                target == null || target == AnnotationUseSiteTarget.PROPERTY

            is IrField ->
                target == AnnotationUseSiteTarget.FIELD || target == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD

            is IrSimpleFunction ->
                target == null || target == AnnotationUseSiteTarget.PROPERTY_GETTER || target == AnnotationUseSiteTarget.PROPERTY_SETTER

            is IrValueParameter ->
                target == null || target == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER

            else -> target == null
        }
}


