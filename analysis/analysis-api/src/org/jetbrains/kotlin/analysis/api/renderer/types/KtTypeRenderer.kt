/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KtContextReceiversRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.KtKeywordsRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KtRendererTypeApproximator
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.*
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KtTypeRenderer private constructor(
    public val expandedTypeRenderingMode: KtExpandedTypeRenderingMode,

    public val capturedTypeRenderer: KtCapturedTypeRenderer,
    public val definitelyNotNullTypeRenderer: KtDefinitelyNotNullTypeRenderer,
    public val dynamicTypeRenderer: KtDynamicTypeRenderer,
    public val flexibleTypeRenderer: KtFlexibleTypeRenderer,
    public val functionalTypeRenderer: KtFunctionalTypeRenderer,
    public val integerLiteralTypeRenderer: KtIntegerLiteralTypeRenderer,
    public val intersectionTypeRenderer: KtIntersectionTypeRenderer,
    public val typeErrorTypeRenderer: KtTypeErrorTypeRenderer,
    public val typeParameterTypeRenderer: KtTypeParameterTypeRenderer,
    public val unresolvedClassErrorTypeRenderer: KtUnresolvedClassErrorTypeRenderer,
    public val usualClassTypeRenderer: KtUsualClassTypeRenderer,

    public val classIdRenderer: KtClassTypeQualifierRenderer,
    public val typeNameRenderer: KtTypeNameRenderer,
    public val typeApproximator: KtRendererTypeApproximator,
    public val typeProjectionRenderer: KtTypeProjectionRenderer,
    public val annotationsRenderer: KtAnnotationRenderer,
    public val contextReceiversRenderer: KtContextReceiversRenderer,
    public val keywordsRenderer: KtKeywordsRenderer,
) {
    public fun renderType(analysisSession: KtAnalysisSession, type: KtType, printer: PrettyPrinter) {
        with(analysisSession) {
            when (expandedTypeRenderingMode) {
                KtExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE -> {
                    renderAbbreviatedType(type, printer)
                }

                KtExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT -> {
                    renderAbbreviatedType(type, printer)
                    renderExpandedTypeComment(type, printer)
                }

                KtExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE -> {
                    renderExpandedType(type, printer)
                }

                KtExpandedTypeRenderingMode.RENDER_EXPANDED_TYPE_WITH_ABBREVIATED_TYPE_COMMENT -> {
                    renderExpandedType(type, printer)
                    renderAbbreviatedTypeComment(type, printer)
                }
            }
        }
    }

    private fun KtAnalysisSession.renderAbbreviatedType(type: KtType, printer: PrettyPrinter) {
        renderTypeAsIs(type.abbreviatedType ?: type, printer)
    }

    private fun KtAnalysisSession.renderExpandedTypeComment(type: KtType, printer: PrettyPrinter) {
        val expandedType = when {
            type.abbreviatedType != null -> type
            else -> return
        }

        printer.append(" /* = ")
        renderTypeAsIs(expandedType, printer)
        printer.append(" */")
    }

    private fun KtAnalysisSession.renderExpandedType(type: KtType, printer: PrettyPrinter) {
        renderTypeAsIs(type, printer)
    }

    private fun KtAnalysisSession.renderAbbreviatedTypeComment(type: KtType, printer: PrettyPrinter) {
        val abbreviatedType = type.abbreviatedType ?: return

        printer.append(" /* from: ")
        renderTypeAsIs(abbreviatedType, printer)
        printer.append(" */")
    }

    /**
     * Renders [type] directly without considering its abbreviation or expansion.
     */
    private fun KtAnalysisSession.renderTypeAsIs(type: KtType, printer: PrettyPrinter) {
        when (type) {
            is KtCapturedType -> capturedTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtFunctionalType -> functionalTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtUsualClassType -> usualClassTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtDefinitelyNotNullType -> definitelyNotNullTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtDynamicType -> dynamicTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtFlexibleType -> flexibleTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtIntegerLiteralType -> integerLiteralTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtIntersectionType -> intersectionTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtTypeParameterType -> typeParameterTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtClassErrorType -> unresolvedClassErrorTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
            is KtTypeErrorType -> typeErrorTypeRenderer.renderType(this, type, this@KtTypeRenderer, printer)
        }
    }

    public fun with(action: Builder.() -> Unit): KtTypeRenderer {
        val renderer = this
        return KtTypeRenderer {
            this.expandedTypeRenderingMode = renderer.expandedTypeRenderingMode
            this.capturedTypeRenderer = renderer.capturedTypeRenderer
            this.definitelyNotNullTypeRenderer = renderer.definitelyNotNullTypeRenderer
            this.dynamicTypeRenderer = renderer.dynamicTypeRenderer
            this.flexibleTypeRenderer = renderer.flexibleTypeRenderer
            this.functionalTypeRenderer = renderer.functionalTypeRenderer
            this.integerLiteralTypeRenderer = renderer.integerLiteralTypeRenderer
            this.intersectionTypeRenderer = renderer.intersectionTypeRenderer
            this.typeErrorTypeRenderer = renderer.typeErrorTypeRenderer
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
        public operator fun invoke(action: Builder.() -> Unit): KtTypeRenderer =
            Builder().apply(action).build()
    }

    public class Builder {
        public lateinit var expandedTypeRenderingMode: KtExpandedTypeRenderingMode
        public lateinit var capturedTypeRenderer: KtCapturedTypeRenderer
        public lateinit var definitelyNotNullTypeRenderer: KtDefinitelyNotNullTypeRenderer
        public lateinit var dynamicTypeRenderer: KtDynamicTypeRenderer
        public lateinit var flexibleTypeRenderer: KtFlexibleTypeRenderer
        public lateinit var functionalTypeRenderer: KtFunctionalTypeRenderer
        public lateinit var integerLiteralTypeRenderer: KtIntegerLiteralTypeRenderer
        public lateinit var intersectionTypeRenderer: KtIntersectionTypeRenderer
        public lateinit var typeErrorTypeRenderer: KtTypeErrorTypeRenderer
        public lateinit var typeParameterTypeRenderer: KtTypeParameterTypeRenderer
        public lateinit var unresolvedClassErrorTypeRenderer: KtUnresolvedClassErrorTypeRenderer
        public lateinit var usualClassTypeRenderer: KtUsualClassTypeRenderer
        public lateinit var classIdRenderer: KtClassTypeQualifierRenderer
        public lateinit var typeNameRenderer: KtTypeNameRenderer
        public lateinit var typeApproximator: KtRendererTypeApproximator
        public lateinit var typeProjectionRenderer: KtTypeProjectionRenderer
        public lateinit var annotationsRenderer: KtAnnotationRenderer
        public lateinit var contextReceiversRenderer: KtContextReceiversRenderer
        public lateinit var keywordsRenderer: KtKeywordsRenderer

        public fun build(): KtTypeRenderer = KtTypeRenderer(
            expandedTypeRenderingMode,
            capturedTypeRenderer,
            definitelyNotNullTypeRenderer,
            dynamicTypeRenderer,
            flexibleTypeRenderer,
            functionalTypeRenderer,
            integerLiteralTypeRenderer,
            intersectionTypeRenderer,
            typeErrorTypeRenderer,
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
