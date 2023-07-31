/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.renderer.ConeIdShortRenderer
import org.jetbrains.kotlin.fir.renderer.ConeTypeRenderer
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualAnnotationsIncompatibilityType

internal object FirExpectActualAnnotationIncompatibilityDiagnosticRenderers {
    @OptIn(SymbolInternals::class)
    val SYMBOL_RENDERER = Renderer<FirBasedSymbol<*>> {
        FirRenderer(
            typeRenderer = ConeTypeRenderer(),
            idRenderer = ConeIdShortRenderer(),
            classMemberRenderer = null,
            bodyRenderer = null,
            annotationRenderer = null,
            modifierRenderer = null,
            contractRenderer = null,
        ).renderElementAsString(it.fir, trim = true)
    }

    val INCOMPATIBILITY = Renderer { incompatibilityType: ExpectActualAnnotationsIncompatibilityType<FirAnnotation> ->
        val sb = StringBuilder("Annotation `")
            .append(renderAnnotation(incompatibilityType.expectAnnotation))
            .append("` ")
        when (incompatibilityType) {
            is ExpectActualAnnotationsIncompatibilityType.MissingOnActual -> {
                sb.append("is missing on actual declaration")
            }
            is ExpectActualAnnotationsIncompatibilityType.DifferentOnActual -> {
                sb.append("has different arguments on actual declaration: `")
                    .append(renderAnnotation(incompatibilityType.actualAnnotation))
                    .append("`")
            }
        }
        sb.toString()
    }

    private fun renderAnnotation(ann: FirAnnotation): String {
        return FirRenderer(
            typeRenderer = ConeTypeRenderer(),
            idRenderer = ConeIdShortRenderer(),
        ).renderElementAsString(ann, trim = true)
    }
}