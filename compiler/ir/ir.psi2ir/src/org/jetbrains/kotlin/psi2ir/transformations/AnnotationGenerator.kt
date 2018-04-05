/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.transformations

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
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
    irElement.acceptVoid(AnnotationGenerator(context))
}

class AnnotationGenerator(private val context: GeneratorContext) : IrElementVisitorVoid {

    private val constantValueGenerator = ConstantValueGenerator(context, this)

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        visitElement(declaration)
        generateAnnotationsForDeclaration(declaration)
    }

    private fun generateAnnotationsForDeclaration(declaration: IrDeclaration) {
        val allAnnotations = declaration.descriptor.annotations.getAllAnnotations()
        val matchingAnnotations = allAnnotations.filter {
            isAnnotationTargetMatchingDeclaration(it.target, declaration)
        }
        matchingAnnotations.mapTo(declaration.annotations) {
            generateAnnotationConstructorCall(it.annotation)
        }
    }

    fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor): IrCall {
        val annotationType = annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor
                ?: throw AssertionError("No declaration descriptor for annotation $annotationDescriptor")
        assert(DescriptorUtils.isAnnotationClass(annotationClassDescriptor)) {
            "Annotation class expected: $annotationClassDescriptor"
        }

        val primaryConstructorDescriptor =
            annotationClassDescriptor.safeAs<ClassDescriptor>()?.unsubstitutedPrimaryConstructor
                    ?: throw AssertionError("No primary constructor for annotation class $annotationClassDescriptor")
        val primaryConstructorSymbol = context.symbolTable.referenceConstructor(primaryConstructorDescriptor)

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
            val argumentValue = annotationDescriptor.allValueArguments[valueParameter.name]
                    ?: throw AssertionError("Annotation $annotationDescriptor missing value argument for $valueParameter")
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
            is IrProperty -> target == null || target == AnnotationUseSiteTarget.PROPERTY

            is IrField -> target == AnnotationUseSiteTarget.FIELD

            is IrSimpleFunction ->
                target == null || element.descriptor.let {
                    when (it) {
                        is PropertyGetterDescriptor -> target == AnnotationUseSiteTarget.PROPERTY_GETTER
                        is PropertySetterDescriptor -> target == AnnotationUseSiteTarget.PROPERTY_SETTER
                        else -> false
                    }
                }

            else -> target == null
        }
}


