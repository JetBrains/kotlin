/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
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

    @KaExperimentalApi
    public object AS_FUNCTIONAL_TYPE : KaFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFunctionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            AS_FUNCTIONAL_TYPE_WITH_OPTIONAL_PARAMETER_NAMES.renderType(analysisSession, type, typeRenderer, printer, false)
        }
    }

    @KaExperimentalApi
    public object AS_FUNCTIONAL_TYPE_WITH_PARAMETER_NAMES : KaFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFunctionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            AS_FUNCTIONAL_TYPE_WITH_OPTIONAL_PARAMETER_NAMES.renderType(analysisSession, type, typeRenderer, printer, true)
        }
    }

    private object AS_FUNCTIONAL_TYPE_WITH_OPTIONAL_PARAMETER_NAMES {
        fun renderType(
            analysisSession: KaSession,
            type: KaFunctionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
            withParameterNames: Boolean
        ): Unit = with(analysisSession) {
            printer {
                val annotationsRendered = checkIfPrinted {
                    typeRenderer.annotationsRenderer.renderAnnotations(analysisSession, type, this)
                }

                if (annotationsRendered) printer.append(" ")
                if (annotationsRendered || type.isMarkedNullable) append("(")
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
                            if (it is KaFunctionType || it is KaDefinitelyNotNullType) printer.append("(")
                            typeRenderer.renderType(analysisSession, it, printer)
                            if (it is KaFunctionType || it is KaDefinitelyNotNullType) printer.append(")")
                            printer.append('.')
                        }
                        printCollection(type.parameters, prefix = "(", postfix = ")") { valueParameter ->
                            if (withParameterNames) {
                                valueParameter.name?.let { name ->
                                    typeRenderer.typeNameRenderer.renderName(analysisSession, name, valueParameter.type, typeRenderer, this)
                                    append(": ")
                                }
                            }
                            typeRenderer.renderType(analysisSession, valueParameter.type, this)
                        }
                        append(" -> ")
                        typeRenderer.renderType(analysisSession, type.returnType, printer)
                    },
                )
                if (annotationsRendered || type.isMarkedNullable) append(")")
                if (type.isMarkedNullable) append("?")
            }
        }
    }

    @KaExperimentalApi
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
                    with(analysisSession) {
                        if (type.isMarkedNullable) {
                            printer.append('?')
                        }
                    }
                },
            )
        }
    }

    @KaExperimentalApi
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

    @KaExperimentalApi
    public object AS_CLASS_TYPE_FOR_REFLECTION_TYPES_WITH_PARAMETER_NAMES : KaFunctionalTypeRenderer {
        override fun renderType(
            analysisSession: KaSession,
            type: KaFunctionType,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            val renderer = if (type.isReflectType) AS_CLASS_TYPE else AS_FUNCTIONAL_TYPE_WITH_PARAMETER_NAMES
            renderer.renderType(analysisSession, type, typeRenderer, printer)
        }
    }
}
