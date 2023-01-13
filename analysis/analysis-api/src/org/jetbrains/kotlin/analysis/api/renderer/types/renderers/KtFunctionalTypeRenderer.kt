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
                    if (type.hasContextReceivers) {
                        contextReceiversRenderer.renderContextReceivers(type, printer)
                    }
                },
                {
                    if (type.isSuspend) {
                        keywordRenderer.renderKeyword(KtTokens.SUSPEND_KEYWORD, type, printer)
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

}
