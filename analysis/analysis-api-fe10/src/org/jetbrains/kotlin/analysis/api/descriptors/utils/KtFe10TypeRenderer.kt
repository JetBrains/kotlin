/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.utils

import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.classId
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.getKtNamedAnnotationArguments
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.maybeLocalClassId
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.builtins.functions.FunctionClassDescriptor
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
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

internal class KtFe10TypeRenderer(private val options: KtTypeRendererOptions, private val isDebugText: Boolean = false) {
    private companion object {
        const val ERROR_TYPE_TEXT = "ERROR_TYPE"
    }

    fun render(type: KotlinType, consumer: PrettyPrinter) {
        consumer.renderType(type)
    }

    private fun KtFe10RendererConsumer.renderType(type: KotlinType) {
        if (isDebugText) {
            renderTypeAnnotationsDebug(type)
        } else {
            renderFe10Annotations(
                type.annotations,
                isSingleLineAnnotations = true,
                renderAnnotationWithShortNames = options.shortQualifiedNames
            ) { classId ->
                classId != StandardClassIds.Annotations.ExtensionFunctionType
            }
        }
        when (val unwrappedType = type.unwrap()) {
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
                        if (!isDebugText && options.renderFunctionType && descriptor is FunctionClassDescriptor) {
                            renderFunctionType(unwrappedType)
                        } else if (descriptor is TypeParameterDescriptor) {
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

    private fun KtFe10RendererConsumer.renderTypeAnnotationsDebug(type: KotlinType) {
        val annotations = type.annotations
            .filter { it.annotationClass?.classId != StandardClassIds.Annotations.ExtensionFunctionType }

        printCollectionIfNotEmpty(annotations, separator = " ", postfix = "  ") { renderTypeAnnotationDebug(it) }
    }

    private fun KtFe10RendererConsumer.renderTypeAnnotationDebug(annotation: AnnotationDescriptor) {
        val namedValues = annotation.getKtNamedAnnotationArguments()
        renderAnnotationDebug(annotation.annotationClass?.classId, namedValues)
    }

    private fun KtFe10RendererConsumer.renderAnnotationDebug(classId: ClassId?, namedValues: List<KtNamedAnnotationValue>) {
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

    private fun KtFe10RendererConsumer.renderConstantValueDebug(value: KtAnnotationValue) {
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

    private fun KtFe10RendererConsumer.renderFlexibleType(type: FlexibleType) {
        if (isDebugText) {
            append("ft<")
            renderType(type.lowerBound)
            append(", ")
            renderType(type.upperBound)
            append(">")
            return
        }

        val lowerBoundText = buildString { renderType(type.lowerBound) }
        val upperBoundText = buildString { renderType(type.upperBound) }
        append(DescriptorRenderer.COMPACT.renderFlexibleType(lowerBoundText, upperBoundText, type.builtIns))
    }

    private fun KtFe10RendererConsumer.renderDefinitelyNotNullType(type: DefinitelyNotNullType) {
        renderType(type.original)
        append(" & Any")
    }

    private fun KtFe10RendererConsumer.renderErrorType() {
        append(ERROR_TYPE_TEXT)
    }

    private fun KtFe10RendererConsumer.renderCapturedType(type: CapturedType) {
        append("CapturedType(")
        renderTypeProjection(type.typeProjection)
        append(")")
    }

    private fun KtFe10RendererConsumer.renderCapturedType(type: NewCapturedType) {
        append("CapturedType(")
        renderTypeProjection(type.constructor.projection)
        append(")")
    }

    private fun KtFe10RendererConsumer.renderTypeVariableType(typeConstructor: NewTypeVariableConstructor) {
        val name = typeConstructor.originalTypeParameter?.name ?: SpecialNames.NO_NAME_PROVIDED
        append("TypeVariable(").append(name.asString()).append(")")
    }

    private fun KtFe10RendererConsumer.renderIntersectionType(typeConstructor: IntersectionTypeConstructor) {
        if (isDebugText) {
            append("it")
        }
        printCollection(typeConstructor.supertypes, separator = " & ", prefix = "(", postfix = ")") { renderType(it) }
    }

    private fun KtFe10RendererConsumer.renderFunctionType(type: SimpleType) {
        val receiverType = type.getReceiverTypeFromFunctionType()
        val valueParameters = type.getValueParameterTypesFromFunctionType()
        val returnType = type.getReturnTypeFromFunctionType()

        if (receiverType != null) {
            renderType(receiverType)
            append(".")
        }
        printCollection(valueParameters, separator = ", ", prefix = "(", postfix = ")") { renderTypeProjection(it) }
        append(" -> ")
        renderType(returnType)
    }

    private fun KtFe10RendererConsumer.renderTypeParameterType(descriptor: TypeParameterDescriptor) {
        append(descriptor.name.render())
    }

    private fun KtFe10RendererConsumer.renderOrdinaryType(type: SimpleType) {
        val nestedType = KtFe10JvmTypeMapperContext.getNestedType(type)
        renderTypeSegment(nestedType.root, isRoot = true)
        printCollectionIfNotEmpty(nestedType.nested, separator = ".", prefix = ".", postfix = "") { renderTypeSegment(it) }
    }

    private fun KtFe10RendererConsumer.renderTypeSegment(typeSegment: PossiblyInnerType, isRoot: Boolean = false) {
        val classifier = typeSegment.classifierDescriptor

        if (isDebugText) {
            append(classifier.maybeLocalClassId.asString())
        } else if (isRoot) {
            val classId = classifier.maybeLocalClassId
            if (!options.shortQualifiedNames && !classId.packageFqName.isRoot) {
                renderFqName(classId.packageFqName)
                append('.')
            }
            renderFqName(classId.relativeClassName)
        } else {
            append(classifier.name.render())
        }

        val arguments = typeSegment.arguments
        printCollectionIfNotEmpty(arguments, separator = ", ", prefix = "<", postfix = ">") { renderTypeProjection(it) }
    }

    private fun KtFe10RendererConsumer.renderFqName(fqName: FqName) {
        printCollection(fqName.pathSegments(), separator = ".") { append(it.render()) }
    }

    private fun KtFe10RendererConsumer.renderTypeProjection(projection: TypeProjection) {
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