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

internal class KaFe10DebugTypeRenderer {
    private companion object {
        const val ERROR_TYPE_TEXT = "ERROR_TYPE"
    }

    fun render(analysisContext: Fe10AnalysisContext, type: KotlinType, printer: PrettyPrinter) {
        with(analysisContext) {
            renderType(type, printer)
        }
    }

    private fun Fe10AnalysisContext.renderType(type: KotlinType, printer: PrettyPrinter) {
        renderTypeAnnotationsDebug(type, printer)
        when (val unwrappedType = type.unwrap()) {
            is DynamicType -> printer.append("dynamic")
            is FlexibleType -> renderFlexibleType(unwrappedType, printer)
            is DefinitelyNotNullType -> renderDefinitelyNotNullType(unwrappedType, printer)
            is ErrorType -> renderErrorType(printer)
            is CapturedType -> renderCapturedType(unwrappedType, printer)
            is NewCapturedType -> renderCapturedType(unwrappedType, printer)
            is AbbreviatedType -> renderType(unwrappedType.abbreviation, printer)
            is SimpleType -> {
                when (val typeConstructor = unwrappedType.constructor) {
                    is NewTypeVariableConstructor -> renderTypeVariableType(typeConstructor, printer)
                    is IntersectionTypeConstructor -> renderIntersectionType(typeConstructor, printer)
                    else -> {
                        val descriptor = unwrappedType.constructor.declarationDescriptor
                        if (descriptor is TypeParameterDescriptor) {
                            renderTypeParameterType(descriptor, printer)
                        } else if (descriptor is ClassifierDescriptorWithTypeParameters) {
                            renderOrdinaryType(unwrappedType, printer)
                        } else {
                            printer.append("ERROR CLASS")
                        }
                    }
                }
            }
        }

        if (type.isMarkedNullable) {
            printer.append("?")
        }
    }

    private fun Fe10AnalysisContext.renderTypeAnnotationsDebug(type: KotlinType, printer: PrettyPrinter) {
        val annotations = type.annotations
            .filter { it.annotationClass?.classId != StandardClassIds.Annotations.ExtensionFunctionType }

        printer.printCollectionIfNotEmpty(annotations, separator = " ", postfix = "  ") {
            renderTypeAnnotationDebug(it, printer)
        }
    }

    private fun Fe10AnalysisContext.renderTypeAnnotationDebug(annotation: AnnotationDescriptor, printer: PrettyPrinter) {
        val namedValues = annotation.getKtNamedAnnotationArguments(this@Fe10AnalysisContext)
        renderAnnotationDebug(annotation.annotationClass?.classId, namedValues, printer)
    }

    private fun renderAnnotationDebug(classId: ClassId?, namedValues: List<KaNamedAnnotationValue>, printer: PrettyPrinter) {
        with(printer) {
            append("@")

            if (classId != null) {
                append("R|")
                renderFqName(classId.asSingleFqName(), printer)
                append("|")
            } else {
                print("<ERROR TYPE REF>")
            }

            printCollection(namedValues, separator = ", ", prefix = "(", postfix = ")") { argument ->
                append(argument.name.render())
                append(" = ")
                renderConstantValueDebug(argument.expression, printer)
            }
        }
    }

    private fun renderConstantValueDebug(value: KaAnnotationValue, printer: PrettyPrinter) {
        when (value) {
            is KaAnnotationValue.NestedAnnotationValue -> {
                renderAnnotationDebug(value.annotation.classId, value.annotation.arguments, printer)
            }

            is KaAnnotationValue.ArrayValue -> {
                printer.printCollection(value.values, separator = ", ", prefix = "[", postfix = "]") {
                    renderConstantValueDebug(it, printer)
                }
            }

            is KaAnnotationValue.EnumEntryValue -> {
                printer.append(value.callableId?.asSingleFqName()?.render())
            }

            is KaAnnotationValue.ConstantValue -> {
                @Suppress("DEPRECATION")
                printer.append(value.value.constantValueKind.asString)
                    .append("(")
                    .append(value.value.value.toString())
                    .append(")")
            }

            is KaAnnotationValue.UnsupportedValue -> {
                printer.append("KaUnsupportedAnnotationValue")
            }

            is KaAnnotationValue.ClassLiteralValue -> {
                printer.append(value.renderAsSourceCode())
            }
        }
    }

    private fun Fe10AnalysisContext.renderFlexibleType(type: FlexibleType, printer: PrettyPrinter) {
        val lowerBoundText = prettyPrint { renderType(type.lowerBound, this@prettyPrint) }
        val upperBoundText = prettyPrint { renderType(type.upperBound, this@prettyPrint) }
        printer.append(DescriptorRenderer.COMPACT.renderFlexibleType(lowerBoundText, upperBoundText, type.builtIns))
    }

    private fun Fe10AnalysisContext.renderDefinitelyNotNullType(type: DefinitelyNotNullType, printer: PrettyPrinter) {
        renderType(type.original, printer)
        printer.append(" & Any")
    }

    private fun renderErrorType(printer: PrettyPrinter) {
        printer.append(ERROR_TYPE_TEXT)
    }

    private fun Fe10AnalysisContext.renderCapturedType(type: CapturedType, printer: PrettyPrinter) {
        with(printer) {
            append("CapturedType(")
            renderTypeProjection(type.typeProjection, printer)
            append(")")
        }
    }

    private fun Fe10AnalysisContext.renderCapturedType(type: NewCapturedType, printer: PrettyPrinter) {
        with(printer) {
            append("CapturedType(")
            renderTypeProjection(type.constructor.projection, printer)
            append(")")
        }
    }

    private fun renderTypeVariableType(typeConstructor: NewTypeVariableConstructor, printer: PrettyPrinter) {
        val name = typeConstructor.originalTypeParameter?.name ?: SpecialNames.NO_NAME_PROVIDED
        printer.append("TypeVariable(").append(name.asString()).append(")")
    }

    private fun Fe10AnalysisContext.renderIntersectionType(typeConstructor: IntersectionTypeConstructor, printer: PrettyPrinter) {
        with(printer) {
            append("it")
            printCollection(typeConstructor.supertypes, separator = " & ", prefix = "(", postfix = ")") {
                renderType(it, printer)
            }
        }
    }

    private fun renderTypeParameterType(descriptor: TypeParameterDescriptor, printer: PrettyPrinter) {
        printer.append(descriptor.name.render())
    }

    private fun Fe10AnalysisContext.renderOrdinaryType(type: SimpleType, printer: PrettyPrinter) {
        val nestedType = KaFe10JvmTypeMapperContext.getNestedType(type)
        renderTypeSegment(nestedType.root, printer)
        printer.printCollectionIfNotEmpty(nestedType.nested, separator = ".", prefix = ".", postfix = "") {
            renderTypeSegment(it, printer)
        }
    }

    private fun Fe10AnalysisContext.renderTypeSegment(typeSegment: PossiblyInnerType, printer: PrettyPrinter) {
        with(printer) {
            val classifier = typeSegment.classifierDescriptor

            append(classifier.maybeLocalClassId.asString())

            val arguments = typeSegment.arguments
            printCollectionIfNotEmpty(arguments, separator = ", ", prefix = "<", postfix = ">") {
                renderTypeProjection(it, printer)
            }
        }
    }

    private fun renderFqName(fqName: FqName, printer: PrettyPrinter) {
        printer.printCollection(fqName.pathSegments(), separator = ".") {
            append(it.render())
        }
    }

    private fun Fe10AnalysisContext.renderTypeProjection(projection: TypeProjection, printer: PrettyPrinter) {
        with(printer) {
            if (projection.isStarProjection) {
                append("*")
            } else {
                when (projection.projectionKind) {
                    Variance.INVARIANT -> renderType(projection.type, printer)
                    Variance.IN_VARIANCE -> {
                        append("in ")
                        renderType(projection.type, printer)
                    }
                    Variance.OUT_VARIANCE -> {
                        append("out ")
                        renderType(projection.type, printer)
                    }
                }
            }
        }
    }
}