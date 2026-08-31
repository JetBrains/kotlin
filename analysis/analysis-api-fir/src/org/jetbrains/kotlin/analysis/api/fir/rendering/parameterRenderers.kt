/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.keyword
import org.jetbrains.kotlin.analysis.api.rendering.punctuation
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.typeParameters
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder

internal fun KaRendererBuilder.pushParameterRenderers() {
    push(ValueParameterListRenderer)
    push(ValueParameterRenderer)
    push(ValueParameterAnnotationsRenderer)
    push(ValueParameterTypeRenderer)
    push(ValueParameterDefaultValueRenderer)
    push(ValueParameterDefaultValueExpressionRenderer)

    push(ContextParameterListRenderer)
    push(ContextParameterRenderer)
    push(ContextParameterAnnotationsRenderer)
    push(ContextParameterTypeRenderer)

    push(TypeParameterListRenderer)
    push(TypeParameterRenderer)
    push(WhereClauseRenderer)
}

private object ValueParameterListRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.ValueParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        val parameters = value.valueParameters
        val isMultiline = parameters.size > 1 && context.valueFor(KaRenderingOption.MultilineValueParameterLists)

        output.group(KaPiece.ValueParameter) {
            output.punctuation("(")
            if (isMultiline) output.indent()

            parameters.forEachIndexed { index, parameter ->
                if (index > 0) {
                    output.punctuation(",")
                    if (!isMultiline) output.space()
                }
                if (isMultiline) output.newLine()
                render(parameter, KaPiece.ValueParameter)
            }

            if (isMultiline) output.unindent().newLine()
            output.punctuation(")")
        }

        return true
    }
}

private object ValueParameterRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.ValueParameterAnnotations)

        // When the primary constructor is rendered in the class header, the parameter is the declaration of the property, so it carries
        // the property's modifiers and its `val`/`var`. Otherwise, the property is rendered as a class member instead.
        val property = correspondingProperty(value)
        if (property != null) {
            render(property, KaPiece.SymbolModifiers)
        }
        render(value, KaPiece.SymbolModifiers)
        if (property != null) {
            keyword(if (property.isVal) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD)
        }

        render(value, KaPiece.SymbolName)
        render(value, KaPiece.ValueParameterType)
        render(value, KaPiece.ValueParameterDefaultValue)

        return true
    }
}

private object ValueParameterAnnotationsRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)

        // A value parameter list has no room for accessors, so every component of a property declared in the primary constructor has its
        // annotations rendered on the parameter (with a use-site target).
        correspondingProperty(value)?.let { correspondingProperty ->
            renderAnnotations(owner = value, holder = correspondingProperty, useSiteTarget = AnnotationUseSiteTarget.PROPERTY)
            correspondingProperty.backingFieldSymbol?.let { backingField ->
                renderAnnotations(owner = value, holder = backingField, useSiteTarget = AnnotationUseSiteTarget.FIELD)
            }
            correspondingProperty.getter?.let { getter ->
                renderAnnotations(owner = value, holder = getter, useSiteTarget = AnnotationUseSiteTarget.PROPERTY_GETTER)
            }
            correspondingProperty.setter?.let { setter ->
                renderAnnotations(owner = value, holder = setter, useSiteTarget = AnnotationUseSiteTarget.PROPERTY_SETTER)
                renderAnnotations(owner = value, holder = setter.parameter, useSiteTarget = AnnotationUseSiteTarget.SETTER_PARAMETER)
            }
        }

        return true
    }
}

/**
 * The property which [parameter] declares in a primary constructor, provided the parameter is the property's rendered declaration.
 * Returns `null` for any other value parameter, and when the property is rendered as a class member instead.
 */
context(context: KaRenderingContext)
private fun correspondingProperty(parameter: KaValueParameterSymbol): KaKotlinPropertySymbol? {
    if (!context.valueFor(KaRenderingOption.PrimaryConstructorInClassHeader)) return null
    return parameter.primaryConstructorProperty
}

private object ValueParameterTypeRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        output.punctuation(":").space()
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object ValueParameterDefaultValueRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterDefaultValue) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        if (!value.hasDefaultValue) return true

        output.space().punctuation("=").space()
        render(value, KaPiece.ValueParameterDefaultValueExpression)
        return true
    }
}

private object ValueParameterDefaultValueExpressionRenderer :
    KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterDefaultValueExpression) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        // The expression is not part of the symbol, so it is rendered as a placeholder.
        output.punctuation("...")
        return true
    }
}

private object ContextParameterListRenderer : KaPieceRenderer<KaCallableSymbol>(KaPiece.ContextParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCallableSymbol, next: () -> Unit): Boolean {
        val contextParameters = value.contextParameters
        if (contextParameters.isEmpty()) {
            return true
        }

        keyword(KtTokens.CONTEXT_KEYWORD, trailingSpace = false)

        output.group(KaPiece.ContextParameter) {
            output.punctuation("(")
            contextParameters.forEachIndexed { index, parameter ->
                if (index > 0) output.punctuation(",").space()
                render(parameter, KaPiece.ContextParameter)
            }
            output.punctuation(")")
        }

        val onNewLine = context.valueFor(KaRenderingOption.ContextReceiversOnNewLine)(session, context, value)
        if (onNewLine) output.newLine() else output.space()

        return true
    }
}

private object ContextParameterRenderer : KaPieceRenderer<KaContextParameterSymbol>(KaPiece.ContextParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.ContextParameterAnnotations)
        render(value, KaPiece.SymbolModifiers)
        render(value, KaPiece.SymbolName)
        render(value, KaPiece.ContextParameterType)

        return true
    }
}

private object ContextParameterAnnotationsRenderer : KaPieceRenderer<KaContextParameterSymbol>(KaPiece.ContextParameterAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextParameterSymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        return true
    }
}

private object ContextParameterTypeRenderer : KaPieceRenderer<KaContextParameterSymbol>(KaPiece.ContextParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextParameterSymbol, next: () -> Unit): Boolean {
        output.punctuation(":").space()
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object TypeParameterListRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.TypeParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        val typeParameters = value.typeParameters
        if (typeParameters.isEmpty()) {
            return true
        }

        output.group(KaPiece.TypeParameter) {
            output.punctuation("<")
            typeParameters.forEachIndexed { index, typeParameter ->
                if (index > 0) output.punctuation(",").space()
                render(typeParameter, KaPiece.TypeParameter)
            }
            output.punctuation(">")
        }

        return true
    }
}

private object TypeParameterRenderer : KaPieceRenderer<KaTypeParameterSymbol>(KaPiece.TypeParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeParameterSymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)

        render(value, KaPiece.SymbolName)

        // A single upper bound is rendered inline; multiple bounds are deferred to a `where` clause.
        if (value.upperBounds.size == 1) {
            output.space().punctuation(":").space()
            render(value.upperBounds.single(), KaPiece.Type)
        }

        return true
    }
}

private object WhereClauseRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.WhereClause) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        // Only type parameters with more than one upper bound contribute to the `where` clause; single bounds are rendered inline.
        val constrainedTypeParameters = value.typeParameters.filter { it.upperBounds.size > 1 }
        if (constrainedTypeParameters.isEmpty()) return true

        output.space()
        keyword(KtTokens.WHERE_KEYWORD)
        var first = true
        for (typeParameter in constrainedTypeParameters) {
            for (bound in typeParameter.upperBounds) {
                if (!first) output.punctuation(",").space()
                first = false
                render(typeParameter, KaPiece.SymbolName)
                output.space().punctuation(":").space()
                render(bound, KaPiece.Type)
            }
        }

        return true
    }
}
