/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

/**
 * Renders a representation of a [KtType] which was expanded from an abbreviated type, given both the original expanded type and its
 * abbreviated type. Depending on the chosen renderer implementation, it usually renders either the expanded type ([AS_EXPANDED_TYPE]) or
 * the abbreviated type ([AS_ABBREVIATED_TYPE]).
 *
 * [KtExpandedTypeRenderer] must not be invoked for [KtType]s without [abbreviated types][KtType.abbreviatedType].
 *
 * [KtTypeRenderer.renderType] invokes this renderer as an essential type rendering step.
 */
public interface KtExpandedTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(expandedType: KtType, abbreviatedType: KtType, printer: PrettyPrinter)

    public object AS_ABBREVIATED_TYPE : KtExpandedTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(
            expandedType: KtType,
            abbreviatedType: KtType,
            printer: PrettyPrinter,
        ) {
            renderType(abbreviatedType, printer)
        }
    }

    public object AS_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT : KtExpandedTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(
            expandedType: KtType,
            abbreviatedType: KtType,
            printer: PrettyPrinter,
        ) {
            renderType(abbreviatedType, printer)
            printer.append(" /* = ")
            renderTypeIgnoringAbbreviation(expandedType, printer)
            printer.append(" */")
        }
    }

    public object AS_EXPANDED_TYPE : KtExpandedTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(
            expandedType: KtType,
            abbreviatedType: KtType,
            printer: PrettyPrinter,
        ) {
            renderTypeIgnoringAbbreviation(expandedType, printer)
        }
    }

    public object AS_EXPANDED_TYPE_WITH_ABBREVIATED_TYPE_COMMENT : KtExpandedTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(
            expandedType: KtType,
            abbreviatedType: KtType,
            printer: PrettyPrinter,
        ) {
            renderTypeIgnoringAbbreviation(expandedType, printer)
            printer.append(" /* from: ")
            renderType(abbreviatedType, printer)
            printer.append(" */")
        }
    }
}
