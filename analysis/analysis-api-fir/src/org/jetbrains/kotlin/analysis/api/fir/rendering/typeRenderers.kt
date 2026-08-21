/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.KaTypeApproximation
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSubtypeOrSelf
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSupertypeOrSelf
import org.jetbrains.kotlin.analysis.api.types.fullyExpandedType
import org.jetbrains.kotlin.analysis.api.types.functionTypeFamily
import org.jetbrains.kotlin.analysis.api.types.hasFlexibleNullability
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder

internal fun KaRendererBuilder.pushTypeRenderers() {
    push(TypeRenderer)
    push(TypeAnnotationsRenderer)
    push(TypeNullabilityRenderer)
    push(ClassTypeRenderer)
    push(ClassTypeNameRenderer)
    push(FunctionTypeRenderer)
    push(FunctionTypeParameterRenderer)
    push(FunctionTypeContextParameterRenderer)
    push(TypeParameterTypeRenderer)
    push(CapturedTypeRenderer)
    push(DefinitelyNotNullTypeRenderer)
    push(FlexibleTypeRenderer)
    push(IntersectionTypeRenderer)
    push(DynamicTypeRenderer)
    push(ErrorTypeRenderer)
    push(TypeArgumentListRenderer)
    push(TypeProjectionRenderer)
}

private object TypeRenderer : KaPieceRenderer<KaType>(KaPiece.Type) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        val type = effectiveType(value)

        when (context.valueFor(KaRenderingOption.ClassTypeRenderingMode)) {
            KaClassTypeRenderingMode.ABBREVIATION -> {
                renderTypeAsIs(type.abbreviationOrSelf)
            }

            KaClassTypeRenderingMode.ABBREVIATION_WITH_EXPANSION_COMMENT -> {
                renderTypeAsIs(type.abbreviationOrSelf)
                expansionForComment(type)?.let { renderTypeComment("= ", it) }
            }

            KaClassTypeRenderingMode.EXPANSION -> {
                renderTypeAsIs(type.fullyExpandedType)
            }

            KaClassTypeRenderingMode.EXPANSION_WITH_ABBREVIATION_COMMENT -> {
                renderTypeAsIs(type.fullyExpandedType)
                abbreviationForComment(type)?.let { renderTypeComment("from: ", it) }
            }
        }
        return true
    }

    /** Renders [type] itself, without applying [KaRenderingOption.ClassTypeRenderingMode] to it again. */
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    private fun renderTypeAsIs(type: KaType) {
        renderTypeAsIs(type, output)
    }

    /** Renders [type] itself into [output], without applying [KaRenderingOption.ClassTypeRenderingMode] to it again. */
    context(session: KaSession, context: KaRenderingContext)
    private fun renderTypeAsIs(type: KaType, output: KaRenderingOutput) {
        when (type) {
            is KaFunctionType -> if (type.isRenderedAsFunctionType) {
                context.render(type, KaPiece.FunctionType, output)
            } else {
                // A reflection function type has no function type syntax, so it is rendered as the class type it is.
                context.render(type, KaPiece.ClassType, output)
            }

            is KaErrorType -> context.render(type, KaPiece.ErrorType, output)
            is KaClassType -> context.render(type, KaPiece.ClassType, output)
            is KaTypeParameterType -> context.render(type, KaPiece.TypeParameterType, output)
            is KaCapturedType -> context.render(type, KaPiece.CapturedType, output)
            is KaDefinitelyNotNullType -> context.render(type, KaPiece.DefinitelyNotNullType, output)
            is KaFlexibleType -> context.render(type, KaPiece.FlexibleType, output)
            is KaIntersectionType -> context.render(type, KaPiece.IntersectionType, output)
            is KaDynamicType -> context.render(type, KaPiece.DynamicType, output)
            else -> output.append("ERROR", KaTextAttribute.Identifier)
        }
    }

    /** The expansion to show in a comment next to [type], or `null` if [type] has no expansion of its own. */
    context(session: KaSession)
    private fun expansionForComment(type: KaType): KaType? {
        return when {
            type.abbreviation != null -> type
            type.symbol is KaTypeAliasSymbol -> type.fullyExpandedType
            else -> null
        }
    }

    /** The abbreviation to show in a comment next to the expansion of [type], or `null` if [type] has no abbreviation. */
    private fun abbreviationForComment(type: KaType): KaType? {
        return type.abbreviation
            ?: type.takeIf { it.symbol is KaTypeAliasSymbol }
    }

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    private fun renderTypeComment(prefix: String, type: KaType) {
        // The type is rendered to plain text first, so that the entire comment is a single text fragment.
        val typeOutput = KaRenderingOutput.plainString()
        renderTypeAsIs(type, typeOutput)
        output.append(" /* $prefix$typeOutput */", KaTextAttribute.Comment)
    }

    /**
     * Applies [KaRenderingOption.TypeTransformation] and [KaRenderingOption.TypeApproximation] to [type].
     *
     * Every rendered type passes through [KaPiece.Type], so applying the options here covers all of them, including nested ones such as
     * type arguments. Approximation of an outer type already makes its arguments denotable, so the nested applications are no-ops.
     */
    context(session: KaSession, context: KaRenderingContext)
    private fun effectiveType(type: KaType): KaType {
        val transformed = context.valueFor(KaRenderingOption.TypeTransformation)(session, context, type)

        return when (context.valueFor(KaRenderingOption.TypeApproximation)) {
            KaTypeApproximation.NONE -> transformed
            KaTypeApproximation.TO_DENOTABLE_SUBTYPE -> transformed.approximateToDenotableSubtypeOrSelf()
            KaTypeApproximation.TO_DENOTABLE_SUPERTYPE -> transformed.approximateToDenotableSupertypeOrSelf(allowLocalDenotableTypes = true)
        }
    }
}

private object TypeAnnotationsRenderer : KaPieceRenderer<KaType>(KaPiece.TypeAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        return true
    }
}

private object TypeNullabilityRenderer : KaPieceRenderer<KaType>(KaPiece.TypeNullability) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        if (value.isMarkedNullable) output.append("?", KaTextAttribute.Punctuation)
        return true
    }
}

private object ClassTypeRenderer : KaPieceRenderer<KaClassType>(KaPiece.ClassType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        render(value, KaPiece.ClassTypeName)
        render(value, KaPiece.TypeNullability)
        return true
    }
}

private object ClassTypeNameRenderer : KaPieceRenderer<KaClassType>(KaPiece.ClassTypeName) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassType, next: () -> Unit): Boolean {
        renderClassTypeQualifiedName(value)
        return true
    }
}

private object FunctionTypeRenderer : KaPieceRenderer<KaFunctionType>(KaPiece.FunctionType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)

        val isParenthesized = value.isMarkedNullable || hasRenderedAnnotations(value)
        if (isParenthesized) output.append("(", KaTextAttribute.Punctuation)

        if (value.isSuspend) keyword(KtTokens.SUSPEND_KEYWORD)
        renderFunctionTypeFamilyPrefix(value)

        val contextReceivers = value.contextReceivers
        if (contextReceivers.isNotEmpty()) {
            keyword(KtTokens.CONTEXT_KEYWORD, trailingSpace = false)
            output.group(KaPiece.FunctionTypeContextParameter) {
                output.append("(", KaTextAttribute.Punctuation)
                contextReceivers.forEachIndexed { index, contextReceiver ->
                    if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                    render(contextReceiver, KaPiece.FunctionTypeContextParameter)
                }
                output.append(")", KaTextAttribute.Punctuation)
            }
            output.space()
        }

        value.receiverType?.let { receiverType ->
            renderReceiverType(receiverType)
            output.append(".", KaTextAttribute.Punctuation)
        }

        output.group(KaPiece.FunctionTypeParameter) {
            output.append("(", KaTextAttribute.Punctuation)
            value.parameters.forEachIndexed { index, parameter ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                render(parameter, KaPiece.FunctionTypeParameter)
            }
            output.append(")", KaTextAttribute.Punctuation)
        }

        output.append(" -> ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)

        if (isParenthesized) output.append(")", KaTextAttribute.Punctuation)
        render(value, KaPiece.TypeNullability)
        return true
    }

    context(session: KaSession, context: KaRenderingContext)
    private fun hasRenderedAnnotations(type: KaFunctionType): Boolean {
        return type.annotations.isNotEmpty() &&
                context.valueFor(KaRenderingOption.Annotations)(session, context, type).isNotEmpty()
    }

    context(session: KaSession, output: KaRenderingOutput)
    private fun renderFunctionTypeFamilyPrefix(type: KaFunctionType) {
        val family = type.functionTypeFamily ?: return
        val prefix = family.typeRenderingPrefix ?: return
        if (prefix == KtTokens.SUSPEND_KEYWORD.value) return

        val markerAnnotationClassId = family.markerAnnotationClassId
        if (markerAnnotationClassId != null && type.annotations.any { it.classId == markerAnnotationClassId }) return

        output.append(prefix, KaTextAttribute.Identifier)
        output.space()
    }
}

private object FunctionTypeParameterRenderer : KaPieceRenderer<KaFunctionValueParameter>(KaPiece.FunctionTypeParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionValueParameter, next: () -> Unit): Boolean {
        // A function type parameter is named by a `@ParameterName` annotation on its type, so most parameters have no name at all.
        value.name?.let { name ->
            output.append(name.render(), KaTextAttribute.Identifier)
            output.append(": ", KaTextAttribute.Punctuation)
        }

        render(value.type, KaPiece.Type)
        return true
    }
}

private object FunctionTypeContextParameterRenderer :
    KaPieceRenderer<KaContextReceiver>(KaPiece.FunctionTypeContextParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextReceiver, next: () -> Unit): Boolean {
        render(value.type, KaPiece.Type)
        return true
    }
}

private object TypeParameterTypeRenderer : KaPieceRenderer<KaTypeParameterType>(KaPiece.TypeParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeParameterType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        identifier(value.name, value.symbol)
        render(value, KaPiece.TypeNullability)
        return true
    }
}

private object CapturedTypeRenderer : KaPieceRenderer<KaCapturedType>(KaPiece.CapturedType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCapturedType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        output.append("Captured", KaTextAttribute.Identifier)
        output.append("(", KaTextAttribute.Punctuation)
        render(value.projection, KaPiece.TypeProjection)
        output.append(")", KaTextAttribute.Punctuation)
        return true
    }
}

private object DefinitelyNotNullTypeRenderer : KaPieceRenderer<KaDefinitelyNotNullType>(KaPiece.DefinitelyNotNullType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDefinitelyNotNullType, next: () -> Unit): Boolean {
        // The original type carries the same annotations, so it renders them.
        render(value.original, KaPiece.Type)
        output.append(" & ", KaTextAttribute.Punctuation)
        output.append("Any", KaTextAttribute.Identifier)
        return true
    }
}

private object FlexibleTypeRenderer : KaPieceRenderer<KaFlexibleType>(KaPiece.FlexibleType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFlexibleType, next: () -> Unit): Boolean {
        val lower = value.lowerBound
        val upper = value.upperBound

        if (context.valueFor(KaRenderingOption.FlexibleTypeShrinking)) {
            // A flexible type whose bounds differ only in nullability (e.g. `String..String?`) is rendered compactly as `String!`.
            // The lower bound carries the same annotations as the flexible type, so it renders them.
            if (isNullabilityFlexibleType(lower, upper)) {
                render(lower, KaPiece.Type)
                output.append("!", KaTextAttribute.Punctuation)
                return true
            }

            // A flexible type between a mutable collection and its read-only counterpart (e.g. `MutableList<String!>..List<String!>?`)
            // is rendered compactly as `(Mutable)List<String!>!`. Unlike the nullability case, the lower bound cannot be rendered as a
            // whole type, as its classifier name carries the marker, so only its type arguments are rendered. A difference between the
            // bounds' type arguments, should one occur, is therefore not visible in the output.
            if (lower is KaClassType && upper is KaClassType && isMutabilityFlexibleType(lower, upper)) {
                // No bound is rendered as a type here, so this is the one form which renders the flexible type's own annotations.
                renderTypeAnnotations(value)
                renderClassTypeQualifiedName(lower, mutabilityFlexible = true)
                if (value.hasFlexibleNullability) {
                    output.append("!", KaTextAttribute.Punctuation)
                } else {
                    // Both bounds agree on nullability, so it is rendered as it is, e.g. `(Mutable)List<String!>?`.
                    render(lower, KaPiece.TypeNullability)
                }
                return true
            }
        }

        // Each bound renders its own annotations, which for the lower bound are those of the flexible type.
        output.append("(", KaTextAttribute.Punctuation)
        render(lower, KaPiece.Type)
        output.append("..", KaTextAttribute.Punctuation)
        render(upper, KaPiece.Type)
        output.append(")", KaTextAttribute.Punctuation)

        return true
    }

    context(session: KaSession)
    private fun isNullabilityFlexibleType(lower: KaType, upper: KaType): Boolean {
        val isSameType = (lower is KaClassType && upper is KaClassType && lower.classId == upper.classId)
                || (lower is KaTypeParameterType && upper is KaTypeParameterType && lower.symbol == upper.symbol)

        if (!isSameType || lower.isMarkedNullable || !upper.isMarkedNullable) return false

        if (lower is KaClassType && upper is KaClassType) {
            val lowerArguments = lower.typeArguments
            val upperArguments = upper.typeArguments
            return lowerArguments.size == upperArguments.size &&
                    lowerArguments.indices.all { upperArguments[it].type == lowerArguments[it].type }
        }

        return true
    }

    private fun isMutabilityFlexibleType(lower: KaClassType, upper: KaClassType): Boolean =
        StandardClassIds.Collections.mutableCollectionToBaseCollection[lower.classId] == upper.classId
}

private object IntersectionTypeRenderer : KaPieceRenderer<KaIntersectionType>(KaPiece.IntersectionType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaIntersectionType, next: () -> Unit): Boolean {
        value.conjuncts.forEachIndexed { index, conjunct ->
            if (index > 0) output.append(" & ", KaTextAttribute.Punctuation)
            render(conjunct, KaPiece.Type)
        }
        return true
    }
}

private object DynamicTypeRenderer : KaPieceRenderer<KaDynamicType>(KaPiece.DynamicType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDynamicType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        keyword(KtTokens.DYNAMIC_KEYWORD, trailingSpace = false)
        return true
    }
}

private object ErrorTypeRenderer : KaPieceRenderer<KaErrorType>(KaPiece.ErrorType) {
    @OptIn(KaNonPublicApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaErrorType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)

        val qualifiers = (value as? KaClassErrorType)?.qualifiers
        if (!qualifiers.isNullOrEmpty()) {
            qualifiers.forEachIndexed { index, qualifier ->
                if (index > 0) output.append(".", KaTextAttribute.Punctuation)
                val symbol = (qualifier as? KaResolvedClassTypeQualifier)?.symbol
                if (symbol != null) {
                    identifier(qualifier.name, symbol)
                } else {
                    output.append(qualifier.name.render(), KaTextAttribute.Identifier)
                }
                if (qualifier.typeArguments.isNotEmpty()) {
                    render(qualifier.typeArguments, KaPiece.TypeArgumentList)
                }
            }
            return true
        }

        output.append(value.presentableText ?: "ERROR", KaTextAttribute.Identifier)
        return true
    }
}

private object TypeArgumentListRenderer : KaPieceRenderer<List<KaTypeProjection>>(KaPiece.TypeArgumentList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: List<KaTypeProjection>, next: () -> Unit): Boolean {
        output.group(KaPiece.TypeProjection) {
            output.append("<", KaTextAttribute.Punctuation)
            value.forEachIndexed { index, projection ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                render(projection, KaPiece.TypeProjection)
            }
            output.append(">", KaTextAttribute.Punctuation)
        }
        return true
    }
}

private object TypeProjectionRenderer : KaPieceRenderer<KaTypeProjection>(KaPiece.TypeProjection) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeProjection, next: () -> Unit): Boolean {
        when (value) {
            is KaStarTypeProjection -> output.append("*", KaTextAttribute.Punctuation)
            is KaTypeArgumentWithVariance -> {
                renderVariance(value.variance)
                render(value.type, KaPiece.Type)
            }
        }
        return true
    }
}

context(context: KaRenderingContext, output: KaRenderingOutput)
private fun renderVariance(variance: Variance) {
    when (variance) {
        Variance.IN_VARIANCE -> keyword(KtTokens.IN_KEYWORD)
        Variance.OUT_VARIANCE -> keyword(KtTokens.OUT_KEYWORD)
        Variance.INVARIANT -> {}
    }
}

/**
 * Renders the qualified name of [type] as prescribed by [KaRenderingOption.ClassTypeQualification], including the type arguments of each
 * rendered qualifier segment. This is the implementation of [KaPiece.ClassTypeName].
 *
 * When [mutabilityFlexible] is `true`, every segment which names a mutable collection is rendered with the mutability-flexible marker
 * instead, e.g. `(Mutable)List` for `kotlin.collections.MutableList`, and `(Mutable)Map.(Mutable)Entry` for
 * `kotlin.collections.MutableMap.MutableEntry`. As the marker cannot be carried by [KaPiece.ClassTypeName], whose value is the class type
 * alone, [FlexibleTypeRenderer] calls this function directly instead of rendering that piece.
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
private fun renderClassTypeQualifiedName(type: KaClassType, mutabilityFlexible: Boolean = false) {
    val qualifiers = type.qualifiers

    if (qualifiers.isEmpty()) {
        // Fallback for class types that expose no explicit qualifier segments.
        identifier(qualifierName(type.classId.shortClassName, mutabilityFlexible), type.symbol)
        if (type.typeArguments.isNotEmpty()) {
            render(type.typeArguments, KaPiece.TypeArgumentList)
        }
        return
    }

    renderQualifiedClassName(type.classId.packageFqName, qualifiers) { qualifier ->
        identifier(qualifierName(qualifier.name, mutabilityFlexible), qualifier.symbol)
        if (qualifier.typeArguments.isNotEmpty()) {
            render(qualifier.typeArguments, KaPiece.TypeArgumentList)
        }
    }
}

/** The rendered name of a single qualifier segment, e.g. `List`, or `(Mutable)List` for a [mutabilityFlexible] `MutableList`. */
private fun qualifierName(name: Name, mutabilityFlexible: Boolean): String {
    // `Name.render()` wraps keywords and names with special characters in backticks.
    val rendered = name.render()
    if (!mutabilityFlexible || !rendered.startsWith(MUTABLE_NAME_PREFIX)) {
        return rendered
    }

    return "($MUTABLE_NAME_PREFIX)" + rendered.removePrefix(MUTABLE_NAME_PREFIX)
}

/** The name prefix shared by all mutable collections in [StandardClassIds.Collections.mutableCollectionToBaseCollection]. */
private const val MUTABLE_NAME_PREFIX = "Mutable"

/**
 * Whether [this] type is rendered with function type syntax, such as `(Int) -> String`.
 *
 * [Reflection function types][KaFunctionType.isReflectType], such as `KFunction1<Int, String>`, are function types in the type system, but
 * they can only be written as class types. Rendering them with function type syntax would drop the very part which distinguishes them from
 * a plain `Function1<Int, String>`.
 */
private val KaType.isRenderedAsFunctionType: Boolean
    get() = this is KaFunctionType && !isReflectType

/**
 * Renders a receiver type of extension callable or a function type, wrapping it in parentheses when required by the Kotlin grammar
 * (function types rendered with function type syntax, and definitely non-nullable types).
 *
 * A nullable type needs no parentheses, e.g. `fun String?.foo()`.
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
internal fun renderReceiverType(type: KaType) {
    val parenthesized = type.isRenderedAsFunctionType || type is KaDefinitelyNotNullType
    if (parenthesized) output.append("(", KaTextAttribute.Punctuation)
    render(type, KaPiece.Type)
    if (parenthesized) output.append(")", KaTextAttribute.Punctuation)
}

context(session: KaSession, context: KaRenderingContext)
private fun renderTypeAnnotations(type: KaType) {
    if (type.annotations.isEmpty()) return
    render(type, KaPiece.TypeAnnotations)
}
