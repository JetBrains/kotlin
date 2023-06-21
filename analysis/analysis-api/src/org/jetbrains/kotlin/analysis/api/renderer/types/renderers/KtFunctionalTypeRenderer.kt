/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtFunctionalType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens


public interface KtFunctionalTypeRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderType(type: KtFunctionalType, printer: PrettyPrinter)

    public object AS_FUNCTIONAL_TYPE : KtFunctionalTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtFunctionalType, printer: PrettyPrinter): Unit = printer {
            val annotationsRendered = checkIfPrinted { annotationsRenderer.renderAnnotations(type, this) }
            if (annotationsRendered) printer.append(" ")
            if (annotationsRendered || type.nullability == KtTypeNullability.NULLABLE) append("(")
            " ".separated(
                {
                    if (type.isSuspend) {
                        keywordsRenderer.renderKeyword(KtTokens.SUSPEND_KEYWORD, type, printer)
                    }
                },
                {
                    if (type.hasContextReceivers) {
                        contextReceiversRenderer.renderContextReceivers(type, printer)
                    }
                },
                {
                    type.receiverType?.let { renderType(it, printer); printer.append('.') }
                    printCollection(type.parameterTypes, prefix = "(", postfix = ")") {
                        renderType(it, this)
                    }
                    append(" -> ")
                    renderType(type.returnType, printer)
                },
            )
            if (annotationsRendered || type.nullability == KtTypeNullability.NULLABLE) append(")")
            if (type.nullability == KtTypeNullability.NULLABLE) append("?")
        }
    }

    public object AS_CLASS_TYPE : KtFunctionalTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtFunctionalType, printer: PrettyPrinter): Unit = printer {
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

    public object AS_CLASS_TYPE_FOR_REFLECTION_TYPES : KtFunctionalTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtFunctionalType, printer: PrettyPrinter) {
            val renderer = if (type.isReflectType) AS_CLASS_TYPE else AS_FUNCTIONAL_TYPE
            renderer.renderType(type, printer)
        }
    }

    public object AS_FULLY_EXPANDED_CLASS_TYPE_FOR_REFELCTION_TYPES : KtFunctionalTypeRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderType(type: KtFunctionalType, printer: PrettyPrinter) {
            val fullyExpandedType = type.fullyExpandedType
            if (fullyExpandedType is KtFunctionalType) {
                val renderer = if (fullyExpandedType.isReflectType) AS_CLASS_TYPE else AS_FUNCTIONAL_TYPE
                renderer.renderType(fullyExpandedType, printer)
            } else {
                renderType(fullyExpandedType, printer)
            }
        }
    }
}
