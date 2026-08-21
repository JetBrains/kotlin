/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder
import org.jetbrains.kotlin.name.StandardClassIds

internal fun KaRendererBuilder.pushAnnotationRenderers() {
    push(AnnotationsRenderer)
    push(AnnotationRenderer)
    push(AnnotationValuesRenderer)
    push(AnnotationValueRenderer)
    push(ConstantValueRenderer)
}

private object AnnotationsRenderer : KaPieceRenderer<Pair<KaAnnotated, AnnotationUseSiteTarget?>>(KaPiece.Annotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: Pair<KaAnnotated, AnnotationUseSiteTarget?>, next: () -> Unit): Boolean {
        val [annotated, useSiteTarget] = value
        renderAnnotations(owner = annotated, holder = annotated, useSiteTarget = useSiteTarget)
        return true
    }
}

private object AnnotationRenderer : KaPieceRenderer<Pair<KaAnnotation, AnnotationUseSiteTarget?>>(KaPiece.Annotation) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: Pair<KaAnnotation, AnnotationUseSiteTarget?>, next: () -> Unit): Boolean {
        val [annotation, useSiteTarget] = value
        output.append("@", KaTextAttribute.Punctuation)
        if (useSiteTarget != null) {
            output.append(useSiteTarget.renderName, KaTextAttribute.Keyword)
            output.append(":", KaTextAttribute.Punctuation)
        }
        val classId = annotation.classId
        if (classId != null) {
            render(classId, KaPiece.ClassName)
        } else {
            output.append("ERROR", KaTextAttribute.Identifier)
        }
        render(annotation, KaPiece.AnnotationValues)
        return true
    }
}

private object AnnotationValuesRenderer : KaPieceRenderer<KaAnnotation>(KaPiece.AnnotationValues) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotation, next: () -> Unit): Boolean {
        val arguments = value.arguments
        if (arguments.isEmpty()) return true

        output.group(KaPiece.AnnotationValue) {
            output.append("(", KaTextAttribute.Punctuation)
            arguments.forEachIndexed { index, argument ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                output.append(argument.name.asString(), KaTextAttribute.Identifier)
                output.append(" = ", KaTextAttribute.Punctuation)
                render(argument.expression, KaPiece.AnnotationValue)
            }
            output.append(")", KaTextAttribute.Punctuation)
        }

        return true
    }
}

private object AnnotationValueRenderer : KaPieceRenderer<KaAnnotationValue>(KaPiece.AnnotationValue) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotationValue, next: () -> Unit): Boolean {
        when (value) {
            is KaAnnotationValue.ConstantValue -> {
                render(value.value, KaPiece.ConstantValue)
            }
            is KaAnnotationValue.EnumEntryValue -> {
                val callableId = value.callableId
                if (callableId != null) {
                    callableId.classId?.let { classId ->
                        render(classId, KaPiece.ClassName)
                        output.append(".", KaTextAttribute.Punctuation)
                    }
                    output.append(callableId.callableName.asString(), KaTextAttribute.Identifier)
                } else {
                    output.append("ERROR", KaTextAttribute.Identifier)
                }
            }
            is KaAnnotationValue.ClassLiteralValue -> {
                val classId = value.classId
                if (classId != null && classId != StandardClassIds.Array) {
                    render(classId, KaPiece.ClassName)
                } else {
                    render(value.type, KaPiece.Type)
                }
                output.append("::", KaTextAttribute.Punctuation)
                output.append("class", KaTextAttribute.Keyword)
            }
            is KaAnnotationValue.NestedAnnotationValue -> {
                render(value.annotation to null, KaPiece.Annotation)
            }
            is KaAnnotationValue.ArrayValue -> {
                output.group(KaPiece.AnnotationValue) {
                    output.append("[", KaTextAttribute.Punctuation)
                    value.values.forEachIndexed { index, element ->
                        if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                        render(element, KaPiece.AnnotationValue)
                    }
                    output.append("]", KaTextAttribute.Punctuation)
                }
            }
            is KaAnnotationValue.UnsupportedValue -> {
                output.append("ERROR", KaTextAttribute.Identifier)
            }
        }
        return true
    }
}

private object ConstantValueRenderer : KaPieceRenderer<KaConstantValue>(KaPiece.ConstantValue) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaConstantValue, next: () -> Unit): Boolean {
        when (value) {
            is KaConstantValue.NullValue, is KaConstantValue.BooleanValue -> {
                output.append(value.render(), KaTextAttribute.Keyword)
            }
            is KaConstantValue.StringValue, is KaConstantValue.CharValue -> {
                output.append(value.render(), KaTextAttribute.StringLiteral)
            }
            is KaConstantValue.ErrorValue -> {
                output.append("ERROR", KaTextAttribute.Identifier)
            }
            else -> {
                output.append(value.render(), KaTextAttribute.NumberLiteral)
            }
        }
        return true
    }
}

/**
 * Renders annotations from [holder] on [owner] with the given [useSiteTarget].
 *
 * [holder] is a component of [owner] whose annotations cannot be rendered on their own declaration, such as the getter of a property
 * declared in a primary constructor. [owner] is the declaration they are rendered on, which decides their layout.
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
internal fun renderAnnotations(owner: KaAnnotated, holder: KaAnnotated, useSiteTarget: AnnotationUseSiteTarget?) {
    var annotations = context.valueFor(KaRenderingOption.Annotations)(session, context, holder)
    if (holder !== owner) {
        // A component shares its annotations with the declaration it belongs to, so an annotation which is written without a use-site
        // target would otherwise be rendered twice: once for the declaration, and once for the component with a use-site target which is
        // not in the source code.
        annotations = annotations.filterNot { annotation -> isDeclaredOn(annotation, owner) }
    }

    if (annotations.isEmpty()) {
        return
    }

    val onNewLine = context.valueFor(KaRenderingOption.AnnotationsOnNewLine)(session, context, owner)
    for (annotation in annotations) {
        render(annotation to useSiteTarget, KaPiece.Annotation)
        if (onNewLine) output.newLine() else output.space()
    }
}

/** Whether [annotation] is written on [owner] itself, and so is already rendered as one of the owner's own annotations. */
private fun isDeclaredOn(annotation: KaAnnotation, owner: KaAnnotated): Boolean {
    val psi = annotation.psi ?: return false
    return owner.annotations.any { it.psi === psi }
}

/** Whether [holder] has any annotation which [renderAnnotations] would render on [owner]. */
context(session: KaSession, context: KaRenderingContext)
internal fun hasRenderedAnnotations(owner: KaAnnotated, holder: KaAnnotated): Boolean {
    val annotations = context.valueFor(KaRenderingOption.Annotations)(session, context, holder)
    return if (holder === owner) {
        annotations.isNotEmpty()
    } else {
        annotations.any { annotation -> !isDeclaredOn(annotation, owner) }
    }
}
