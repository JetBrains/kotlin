/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KaContextReceiversRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.KaKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.*
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KaTypeRenderer private constructor(
    public val expandedTypeRenderingMode: KaExpandedTypeRenderingMode,

    public val capturedTypeRenderer: KaCapturedTypeRenderer,
    public val definitelyNotNullTypeRenderer: KaDefinitelyNotNullTypeRenderer,
    public val dynamicTypeRenderer: KaDynamicTypeRenderer,
    public val flexibleTypeRenderer: KaFlexibleTypeRenderer,
    public val functionalTypeRenderer: KaFunctionalTypeRenderer,
    public val intersectionTypeRenderer: KaIntersectionTypeRenderer,
    public val errorTypeRenderer: KaErrorTypeRenderer,
    public val typeParameterTypeRenderer: KaTypeParameterTypeRenderer,
    public val unresolvedClassErrorTypeRenderer: KaUnresolvedClassErrorTypeRenderer,
    public val usualClassTypeRenderer: KaUsualClassTypeRenderer,

    public val classIdRenderer: KaClassTypeQualifierRenderer,
    public val typeNameRenderer: KaTypeNameRenderer,
    public val typeApproximator: KaRendererTypeApproximator,
    public val typeProjectionRenderer: KaTypeProjectionRenderer,
    public val annotationsRenderer: KaAnnotationRenderer,
    public val contextReceiversRenderer: KaContextReceiversRenderer,
    public val keywordsRenderer: KaKeywordsRenderer,
) {
    public fun renderType(analysisSession: KaSession, type: KaType, printer: PrettyPrinter) {
        with(analysisSession) {
            when (expandedTypeRenderingMode) {
                KaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE -> {
                    renderAbbreviatedType(type, printer)
                }

                KaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT -> {
                    renderAbbreviatedType(type, printer)
                    renderExpandedTypeComment(type, printer)
                }

                KaExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE -> {
                    renderExpandedType(type, printer)
                }

                KaExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE_WITH_ABBREVIATED_TYPE_COMMENT -> {
                    renderExpandedType(type, printer)
                    renderAbbreviatedTypeComment(type, printer)
                }
            }
        }
    }

    private fun KaSession.renderAbbreviatedType(type: KaType, printer: PrettyPrinter) {
        renderTypeAsIs(type.abbreviatedType ?: type, printer)
    }

    private fun KaSession.renderExpandedTypeComment(type: KaType, printer: PrettyPrinter) {
        val expandedType = when {
            type.abbreviatedType != null -> type
            type.symbol is KaTypeAliasSymbol -> type.fullyExpandedType
            else -> return
        }

        printer.append(" /* = ")
        renderTypeAsIs(expandedType, printer)
        printer.append(" */")
    }

    private fun KaSession.renderExpandedType(type: KaType, printer: PrettyPrinter) {
        renderTypeAsIs(type.fullyExpandedType, printer)
    }

    private fun KaSession.renderAbbreviatedTypeComment(type: KaType, printer: PrettyPrinter) {
        val abbreviatedType = type.abbreviatedType
            ?: type.takeIf { it.symbol is KaTypeAliasSymbol }
            ?: return

        printer.append(" /* from: ")
        renderTypeAsIs(abbreviatedType, printer)
        printer.append(" */")
    }

    /**
     * Renders [type] directly without considering its abbreviation or expansion.
     */
    private fun KaSession.renderTypeAsIs(type: KaType, printer: PrettyPrinter) {
        when (type) {
            is KaCapturedType -> capturedTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaFunctionalType -> functionalTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaUsualClassType -> usualClassTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaDefinitelyNotNullType -> definitelyNotNullTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaDynamicType -> dynamicTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaFlexibleType -> flexibleTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaIntersectionType -> intersectionTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaTypeParameterType -> typeParameterTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaClassErrorType -> unresolvedClassErrorTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
            is KaErrorType -> errorTypeRenderer.renderType(this, type, this@KaTypeRenderer, printer)
        }
    }

    public fun with(action: Builder.() -> Unit): KaTypeRenderer {
        val renderer = this
        return KaTypeRenderer {
            this.expandedTypeRenderingMode = renderer.expandedTypeRenderingMode
            this.capturedTypeRenderer = renderer.capturedTypeRenderer
            this.definitelyNotNullTypeRenderer = renderer.definitelyNotNullTypeRenderer
            this.dynamicTypeRenderer = renderer.dynamicTypeRenderer
            this.flexibleTypeRenderer = renderer.flexibleTypeRenderer
            this.functionalTypeRenderer = renderer.functionalTypeRenderer
            this.intersectionTypeRenderer = renderer.intersectionTypeRenderer
            this.errorTypeRenderer = renderer.errorTypeRenderer
            this.typeParameterTypeRenderer = renderer.typeParameterTypeRenderer
            this.unresolvedClassErrorTypeRenderer = renderer.unresolvedClassErrorTypeRenderer
            this.usualClassTypeRenderer = renderer.usualClassTypeRenderer
            this.classIdRenderer = renderer.classIdRenderer
            this.typeNameRenderer = renderer.typeNameRenderer
            this.typeApproximator = renderer.typeApproximator
            this.typeProjectionRenderer = renderer.typeProjectionRenderer
            this.annotationsRenderer = renderer.annotationsRenderer
            this.contextReceiversRenderer = renderer.contextReceiversRenderer
            this.keywordsRenderer = renderer.keywordsRenderer
            action()
        }
    }

    public companion object {
        public operator fun invoke(action: Builder.() -> Unit): KaTypeRenderer =
            Builder().apply(action).build()
    }

    public class Builder {
        public lateinit var expandedTypeRenderingMode: KaExpandedTypeRenderingMode
        public lateinit var capturedTypeRenderer: KaCapturedTypeRenderer
        public lateinit var definitelyNotNullTypeRenderer: KaDefinitelyNotNullTypeRenderer
        public lateinit var dynamicTypeRenderer: KaDynamicTypeRenderer
        public lateinit var flexibleTypeRenderer: KaFlexibleTypeRenderer
        public lateinit var functionalTypeRenderer: KaFunctionalTypeRenderer
        public lateinit var intersectionTypeRenderer: KaIntersectionTypeRenderer
        public lateinit var errorTypeRenderer: KaErrorTypeRenderer
        public lateinit var typeParameterTypeRenderer: KaTypeParameterTypeRenderer
        public lateinit var unresolvedClassErrorTypeRenderer: KaUnresolvedClassErrorTypeRenderer
        public lateinit var usualClassTypeRenderer: KaUsualClassTypeRenderer
        public lateinit var classIdRenderer: KaClassTypeQualifierRenderer
        public lateinit var typeNameRenderer: KaTypeNameRenderer
        public lateinit var typeApproximator: KaRendererTypeApproximator
        public lateinit var typeProjectionRenderer: KaTypeProjectionRenderer
        public lateinit var annotationsRenderer: KaAnnotationRenderer
        public lateinit var contextReceiversRenderer: KaContextReceiversRenderer
        public lateinit var keywordsRenderer: KaKeywordsRenderer

        public fun build(): KaTypeRenderer = KaTypeRenderer(
            expandedTypeRenderingMode,
            capturedTypeRenderer,
            definitelyNotNullTypeRenderer,
            dynamicTypeRenderer,
            flexibleTypeRenderer,
            functionalTypeRenderer,
            intersectionTypeRenderer,
            errorTypeRenderer,
            typeParameterTypeRenderer,
            unresolvedClassErrorTypeRenderer,
            usualClassTypeRenderer,
            classIdRenderer,
            typeNameRenderer,
            typeApproximator,
            typeProjectionRenderer,
            annotationsRenderer,
            contextReceiversRenderer,
            keywordsRenderer,
        )
    }
}

public typealias KtTypeRenderer = KaTypeRenderer