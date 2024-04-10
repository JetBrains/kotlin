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
    public fun renderType(
        analysisSession: KtAnalysisSession,
        type: KtFunctionalType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_FUNCTIONAL_TYPE : KtFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtFunctionalType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val annotationsRendered = checkIfPrinted {
                    typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this)
                }

                if (annotationsRendered) printer.append(" ")
                if (annotationsRendered || type.nullability == KtTypeNullability.NULLABLE) append("(")
                " ".separated(
                    {
                        if (type.isSuspend) {
                            typeRenderer.keywordsRenderer.renderKeyword(analysisSession, KtTokens.SUSPEND_KEYWORD, type, printer)
                        }
                    },
                    {
                        if (type.hasContextReceivers) {
                            typeRenderer.contextReceiversRenderer.renderContextReceivers(analysisSession, type, typeRenderer, printer)
                        }
                    },
                    {
                        type.receiverType?.let { typeRenderer.renderType(analysisSession, it, printer); printer.append('.') }
                        printCollection(type.parameterTypes, prefix = "(", postfix = ")") {
                            typeRenderer.renderType(analysisSession, it, this)
                        }
                        append(" -> ")
                        typeRenderer.renderType(analysisSession, type.returnType, printer)
                    },
                )
                if (annotationsRendered || type.nullability == KtTypeNullability.NULLABLE) append(")")
                if (type.nullability == KtTypeNullability.NULLABLE) append("?")
            }
        }
    }

    public object AS_CLASS_TYPE : KtFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtFunctionalType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ): Unit = printer {
            " ".separated(
                { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                {
                    typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, typeRenderer, printer)
                    if (type.nullability == KtTypeNullability.NULLABLE) {
                        append('?')
                    }
                },
            )
        }
    }

    public object AS_CLASS_TYPE_FOR_REFLECTION_TYPES : KtFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtFunctionalType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            val renderer = if (type.isReflectType) AS_CLASS_TYPE else AS_FUNCTIONAL_TYPE
            renderer.renderType(analysisSession, type, typeRenderer, printer)
        }
    }

    public object AS_FULLY_EXPANDED_CLASS_TYPE_FOR_REFELCTION_TYPES : KtFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KtAnalysisSession,
            type: KtFunctionalType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            with(analysisSession) {
                val fullyExpandedType = type.fullyExpandedType
                if (fullyExpandedType is KtFunctionalType) {
                    val renderer = if (fullyExpandedType.isReflectType) AS_CLASS_TYPE else AS_FUNCTIONAL_TYPE
                    renderer.renderType(analysisSession, fullyExpandedType, typeRenderer, printer)
                } else {
                    typeRenderer.renderType(analysisSession, fullyExpandedType, printer)
                }
            }
        }
    }
}
