/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.renderer.render

public interface KtClassTypeQualifierRenderer {
    public fun renderClassTypeQualifier(
        analysisSession: KtAnalysisSession,
        type: KtClassType,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_SHORT_NAMES : KtClassTypeQualifierRenderer {
        override fun renderClassTypeQualifier(
            analysisSession: KtAnalysisSession,
            type: KtClassType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            type.qualifiers.last().render(analysisSession, type, typeRenderer, printer)
        }
    }

    public object WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS : KtClassTypeQualifierRenderer {
        override fun renderClassTypeQualifier(
            analysisSession: KtAnalysisSession,
            type: KtClassType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                printCollection(type.qualifiers, separator = ".") { qualifier ->
                    qualifier.render(analysisSession, type, typeRenderer, printer)
                }
            }
        }
    }

    public object WITH_QUALIFIED_NAMES : KtClassTypeQualifierRenderer {
        override fun renderClassTypeQualifier(
            analysisSession: KtAnalysisSession,
            type: KtClassType,
            typeRenderer: KtTypeRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                ".".separated(
                    {
                        if (type is KtNonErrorClassType && type.classId.packageFqName != CallableId.PACKAGE_FQ_NAME_FOR_LOCAL) {
                            append(type.classId.packageFqName.render())
                        }
                    },
                    { WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS.renderClassTypeQualifier(analysisSession, type, typeRenderer, printer) },
                )
            }
        }
    }
}

private fun KtClassTypeQualifier.render(
    analysisSession: KtAnalysisSession,
    type: KtType,
    typeRenderer: KtTypeRenderer,
    printer: PrettyPrinter,
) {
    printer {
        typeRenderer.typeNameRenderer.renderName(analysisSession, name, type, typeRenderer, printer)
        printCollectionIfNotEmpty(typeArguments, prefix = "<", postfix = ">") {
            typeRenderer.typeProjectionRenderer.renderTypeProjection(analysisSession, it, typeRenderer, this)
        }
    }
}
