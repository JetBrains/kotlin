/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.lexer.KtTokens

@KaExperimentalApi
public interface KaFunctionalTypeRenderer {
    public fun renderType(
        analysisSession: KaSession,
        type: KaFunctionType,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_FUNCTIONAL_TYPE : KaFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFunctionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val annotationsRendered = checkIfPrinted {
                    typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this)
                }

                if (annotationsRendered) printer.append(" ")
                if (annotationsRendered || type.nullability == KaTypeNullability.NULLABLE) append("(")
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
                        type.receiverType?.let {
                            if (it is KaFunctionType) printer.append("(")
                            typeRenderer.renderType(analysisSession, it, printer)
                            if (it is KaFunctionType) printer.append(")")
                            printer.append('.')
                        }
                        printCollection(type.parameterTypes, prefix = "(", postfix = ")") {
                            typeRenderer.renderType(analysisSession, it, this)
                        }
                        append(" -> ")
                        typeRenderer.renderType(analysisSession, type.returnType, printer)
                    },
                )
                if (annotationsRendered || type.nullability == KaTypeNullability.NULLABLE) append(")")
                if (type.nullability == KaTypeNullability.NULLABLE) append("?")
            }
        }
    }

    public object AS_CLASS_TYPE : KaFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFunctionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ): Unit = printer {
            " ".separated(
                { typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, printer) },
                {
                    typeRenderer.classIdRenderer.renderClassTypeQualifier(analysisSession, type, type.qualifiers, typeRenderer, printer)
                    if (type.nullability == KaTypeNullability.NULLABLE) {
                        append('?')
                    }
                },
            )
        }
    }

    public object AS_CLASS_TYPE_FOR_REFLECTION_TYPES : KaFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFunctionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            val renderer = if (type.isReflectType) AS_CLASS_TYPE else AS_FUNCTIONAL_TYPE
            renderer.renderType(analysisSession, type, typeRenderer, printer)
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaFunctionalTypeRenderer' instead", ReplaceWith("KaFunctionalTypeRenderer"))
public typealias KtFunctionalTypeRenderer = KaFunctionalTypeRenderer