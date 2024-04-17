/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.api.types.KtUsualClassType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter


public interface KtUsualClassTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(type: KtUsualClassType, printer: PrettyPrinter)

    public object AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS : KtUsualClassTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtUsualClassType, printer: PrettyPrinter): Unit = printer {
            " ".separated(
                { annotationsRenderer.renderAnnotations(type, printer) },
                {
                    classIdRenderer.renderClassTypeQualifier(type, printer)
                    if (type.nullability == KtTypeNullability.NULLABLE) {
                        append('?')
                    }
                },
            )
        }
    }

    /**
     * Renders class type and in case of type alias adds a comment containing fully expanded class type, for example:
     * ```
     * typealias MyInt = Int
     * ```
     * `MyInt` is rendered as `MyInt /* = Int */`
     */
    public object AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS_VERBOSE : KtUsualClassTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtUsualClassType, printer: PrettyPrinter): Unit = printer {
            AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS.renderType(type, printer)
            if (type.classSymbol is KtTypeAliasSymbol) {
                append(" /* = ")
                renderType(type.fullyExpandedType, printer)
                append(" */")
            }
        }
    }

    public object AS_FULLY_EXPANDED_CLASS_TYPE_WITH_TYPE_ARGUMENTS : KtUsualClassTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtUsualClassType, printer: PrettyPrinter) {
            val fullyExpandedType = type.fullyExpandedType
            if (fullyExpandedType is KtUsualClassType) {
                AS_CLASS_TYPE_WITH_TYPE_ARGUMENTS.renderType(fullyExpandedType, printer)
            } else {
                renderType(fullyExpandedType, printer)
            }
        }
    }
}
