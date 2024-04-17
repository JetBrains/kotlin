/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

public interface KtAnnotationUseSiteTargetRenderer {
    context(KtAnalysisSession, KtAnnotationRenderer)
    public fun renderUseSiteTarget(annotation: KtAnnotationApplication, owner: KtAnnotated, printer: PrettyPrinter)

    public object WITHOUT_USE_SITE : KtAnnotationUseSiteTargetRenderer {
        context(KtAnalysisSession, KtAnnotationRenderer)
        override fun renderUseSiteTarget(annotation: KtAnnotationApplication, owner: KtAnnotated, printer: PrettyPrinter) {
        }
    }

    public object WITH_USES_SITE : KtAnnotationUseSiteTargetRenderer {
        context(KtAnalysisSession, KtAnnotationRenderer)
        override fun renderUseSiteTarget(annotation: KtAnnotationApplication, owner: KtAnnotated, printer: PrettyPrinter) {
            val useSite = annotation.useSiteTarget ?: return
            printer.append(useSite.renderName)
            printer.append(':')
        }
    }

    public object WITH_NON_DEFAULT_USE_SITE : KtAnnotationUseSiteTargetRenderer {
        context(KtAnalysisSession, KtAnnotationRenderer)
        override fun renderUseSiteTarget(annotation: KtAnnotationApplication, owner: KtAnnotated, printer: PrettyPrinter) {
            val print = when (owner) {
                is KtReceiverParameterSymbol -> true
                !is KtCallableSymbol -> return
                is KtAnonymousFunctionSymbol -> true
                is KtConstructorSymbol -> true
                is KtFunctionSymbol -> true
                is KtPropertyGetterSymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_GETTER
                is KtPropertySetterSymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_SETTER
                is KtSamConstructorSymbol -> true
                is KtBackingFieldSymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.FIELD
                is KtEnumEntrySymbol -> true
                is KtValueParameterSymbol ->
                    owner.getContainingSymbol() !is KtPropertySetterSymbol || annotation.useSiteTarget != AnnotationUseSiteTarget.SETTER_PARAMETER

                is KtJavaFieldSymbol -> true
                is KtLocalVariableSymbol -> true
                is KtPropertySymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.PROPERTY
                else -> return
            }

            if (print) {
                WITH_USES_SITE.renderUseSiteTarget(annotation, owner, printer)
            }
        }
    }
}