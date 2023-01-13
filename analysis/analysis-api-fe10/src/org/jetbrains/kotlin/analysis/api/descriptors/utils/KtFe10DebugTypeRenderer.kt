/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.descriptors.Fe10AnalysisContext
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getKtNamedAnnotationArguments
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.analysis.utils.printer.prettyPrint
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.PossiblyInnerType
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.checker.NewTypeVariableConstructor
import org.jetbrains.kotlin.types.error.ErrorType
import org.jetbrains.kotlin.types.typeUtil.builtIns

internal class KtFe10DebugTypeRenderer {
    private companion object {
        const val ERROR_TYPE_TEXT = "ERROR_TYPE"
    }

    context(Fe10AnalysisContext)
    fun render(type: KotlinType, consumer: PrettyPrinter) {
        consumer.renderType(type)
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderType(type: KotlinType) {
        renderTypeAnnotationsDebug(type)
        when (val unwrappedType = type.unwrap()) {
            is DynamicType -> append("dynamic")
            is FlexibleType -> renderFlexibleType(unwrappedType)
            is DefinitelyNotNullType -> renderDefinitelyNotNullType(unwrappedType)
            is ErrorType -> renderErrorType()
            is CapturedType -> renderCapturedType(unwrappedType)
            is NewCapturedType -> renderCapturedType(unwrappedType)
            is AbbreviatedType -> renderType(unwrappedType.abbreviation)
            is SimpleType -> {
                when (val typeConstructor = unwrappedType.constructor) {
                    is NewTypeVariableConstructor -> renderTypeVariableType(typeConstructor)
                    is IntersectionTypeConstructor -> renderIntersectionType(typeConstructor)
                    else -> {
                        val descriptor = unwrappedType.constructor.declarationDescriptor
                        if (descriptor is TypeParameterDescriptor) {
                            renderTypeParameterType(descriptor)
                        } else if (descriptor is ClassifierDescriptorWithTypeParameters) {
                            renderOrdinaryType(unwrappedType)
                        } else {
                            append("ERROR CLASS")
                        }
                    }
                }
            }
        }

        if (type.isMarkedNullable) {
            append("?")
        }
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderTypeAnnotationsDebug(type: KotlinType) {
        val annotations = type.annotations
            .filter { it.annotationClass?.classId != StandardClassIds.Annotations.ExtensionFunctionType }

        printCollectionIfNotEmpty(annotations, separator = " ", postfix = "  ") { renderTypeAnnotationDebug(it) }
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderTypeAnnotationDebug(annotation: AnnotationDescriptor) {
        val namedValues = annotation.getKtNamedAnnotationArguments(this@Fe10AnalysisContext)
        renderAnnotationDebug(annotation.annotationClass?.classId, namedValues)
    }

    private fun PrettyPrinter.renderAnnotationDebug(classId: ClassId?, namedValues: List<KtNamedAnnotationValue>) {
        append("@")

        if (classId != null) {
            append("R|")
            renderFqName(classId.asSingleFqName())
            append("|")
        } else {
            print("<ERROR TYPE REF>")
        }

        printCollection(namedValues, separator = ", ", prefix = "(", postfix = ")") { (name, value) ->
            append(name.render())
            append(" = ")
            renderConstantValueDebug(value)
        }
    }

    private fun PrettyPrinter.renderConstantValueDebug(value: KtAnnotationValue) {
        when (value) {
            is KtAnnotationApplicationValue -> renderAnnotationDebug(value.annotationValue.classId, value.annotationValue.arguments)
            is KtArrayAnnotationValue ->
                printCollection(value.values, separator = ", ", prefix = "[", postfix = "]") { renderConstantValueDebug(it) }
            is KtEnumEntryAnnotationValue -> append(value.callableId?.asSingleFqName()?.render())
            is KtConstantAnnotationValue -> append(value.constantValue.constantValueKind.asString).append("(").append(value.constantValue.value.toString()).append(")")
            KtUnsupportedAnnotationValue -> append(KtUnsupportedAnnotationValue::class.java.simpleName)
            is KtKClassAnnotationValue -> append(value.renderAsSourceCode())
        }
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderFlexibleType(type: FlexibleType) {
        val lowerBoundText = prettyPrint { renderType(type.lowerBound) }
        val upperBoundText = prettyPrint { renderType(type.upperBound) }
        append(DescriptorRenderer.COMPACT.renderFlexibleType(lowerBoundText, upperBoundText, type.builtIns))
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderDefinitelyNotNullType(type: DefinitelyNotNullType) {
        renderType(type.original)
        append(" & Any")
    }

    private fun PrettyPrinter.renderErrorType() {
        append(ERROR_TYPE_TEXT)
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderCapturedType(type: CapturedType) {
        append("CapturedType(")
        renderTypeProjection(type.typeProjection)
        append(")")
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderCapturedType(type: NewCapturedType) {
        append("CapturedType(")
        renderTypeProjection(type.constructor.projection)
        append(")")
    }

    private fun PrettyPrinter.renderTypeVariableType(typeConstructor: NewTypeVariableConstructor) {
        val name = typeConstructor.originalTypeParameter?.name ?: SpecialNames.NO_NAME_PROVIDED
        append("TypeVariable(").append(name.asString()).append(")")
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderIntersectionType(typeConstructor: IntersectionTypeConstructor) {
        append("it")
        printCollection(typeConstructor.supertypes, separator = " & ", prefix = "(", postfix = ")") { renderType(it) }
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderFunctionType(type: SimpleType) {
        if (type.isSuspendFunctionType || type.isKSuspendFunctionType) {
            append("suspend ")
        }
        val (receiverType, valueParameters, returnType) = when {
            type.isKFunctionType || type.isKSuspendFunctionType -> Triple(
                null,
                type.arguments.dropLast(1),
                type.arguments.last().type,
            )
            else -> Triple(
                type.getReceiverTypeFromFunctionType(),
                type.getValueParameterTypesFromFunctionType(),
                type.getReturnTypeFromFunctionType()
            )
        }

        if (receiverType != null) {
            renderType(receiverType)
            append(".")
        }
        printCollection(valueParameters, separator = ", ", prefix = "(", postfix = ")") { renderTypeProjection(it) }
        append(" -> ")
        renderType(returnType)
    }

    private fun PrettyPrinter.renderTypeParameterType(descriptor: TypeParameterDescriptor) {
        append(descriptor.name.render())
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderOrdinaryType(type: SimpleType) {
        val nestedType = KtFe10JvmTypeMapperContext.getNestedType(type)
        renderTypeSegment(nestedType.root)
        printCollectionIfNotEmpty(nestedType.nested, separator = ".", prefix = ".", postfix = "") { renderTypeSegment(it) }
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderTypeSegment(typeSegment: PossiblyInnerType) {
        val classifier = typeSegment.classifierDescriptor

        append(classifier.maybeLocalClassId.asString())

        val arguments = typeSegment.arguments
        printCollectionIfNotEmpty(arguments, separator = ", ", prefix = "<", postfix = ">") { renderTypeProjection(it) }
    }

    private fun PrettyPrinter.renderFqName(fqName: FqName) {
        printCollection(fqName.pathSegments(), separator = ".") { append(it.render()) }
    }

    context(Fe10AnalysisContext)
    private fun PrettyPrinter.renderTypeProjection(projection: TypeProjection) {
        if (projection.isStarProjection) {
            append("*")
        } else {
            when (projection.projectionKind) {
                Variance.INVARIANT -> renderType(projection.type)
                Variance.IN_VARIANCE -> {
                    append("in ")
                    renderType(projection.type)
                }
                Variance.OUT_VARIANCE -> {
                    append("out ")
                    renderType(projection.type)
                }
            }
        }
    }
}