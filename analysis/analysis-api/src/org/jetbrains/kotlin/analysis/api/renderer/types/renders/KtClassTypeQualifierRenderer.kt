/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.renders

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

public interface KtClassTypeQualifierRenderer {
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter)

    public object WITH_SHORT_NAMES : KtClassTypeQualifierRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter) {
            typeNameRenderer.renderName(type.qualifiers.last().name, type, printer)
        }
    }

    public object WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS : KtClassTypeQualifierRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter): Unit = printer {
            printCollection(type.qualifiers, separator = ".") { qualifier ->
                typeNameRenderer.renderName(qualifier.name, type, printer)
                printCollectionIfNotEmpty(qualifier.typeArguments, prefix = "<", postfix = ">") {
                    typeProjectionRenderer.renderTypeProjection(it, this)
                }
            }
        }
    }

    public object WITH_QUALIFIED_NAMES : KtClassTypeQualifierRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter): Unit = printer {
            ".".separated(
                {
                    if (type is KtNonErrorClassType) {
                        append(type.classId.packageFqName.render())
                    }
                },
                { WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS.renderClassTypeQualifier(type, printer) },
            )
        }
    }
}
