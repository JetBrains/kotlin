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
    context(KtAnalysisSession, KtTypeRenderer)
    public fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter)

    public object WITH_SHORT_NAMES : KtClassTypeQualifierRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter) {
            type.qualifiers.last().render(type, printer)
        }
    }

    public object WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS : KtClassTypeQualifierRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter): Unit = printer {
            printCollection(type.qualifiers, separator = ".") { qualifier -> qualifier.render(type, printer) }
        }
    }

    public object WITH_QUALIFIED_NAMES : KtClassTypeQualifierRenderer {
        context(KtAnalysisSession, KtTypeRenderer)
        override fun renderClassTypeQualifier(type: KtClassType, printer: PrettyPrinter): Unit = printer {
            ".".separated(
                {
                    if (type is KtNonErrorClassType && type.classId.packageFqName != CallableId.PACKAGE_FQ_NAME_FOR_LOCAL) {
                        append(type.classId.packageFqName.render())
                    }
                },
                { WITH_SHORT_NAMES_WITH_NESTED_CLASSIFIERS.renderClassTypeQualifier(type, printer) },
            )
        }
    }
}

context(KtAnalysisSession, KtTypeRenderer)
private fun KtClassTypeQualifier.render(type: KtType, printer: PrettyPrinter) = printer {
    typeNameRenderer.renderName(name, type, printer)
    printCollectionIfNotEmpty(typeArguments, prefix = "<", postfix = ">") {
        typeProjectionRenderer.renderTypeProjection(it, this)
    }
}
