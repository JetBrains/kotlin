/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRenderer
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget

@KaExperimentalApi
public interface KaAnnotationUseSiteTargetRenderer {
    public fun renderUseSiteTarget(
        analysisSession: KaSession,
        annotation: KaAnnotation,
        owner: KaAnnotated,
        annotationRenderer: KaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    public object WITHOUT_USE_SITE : KaAnnotationUseSiteTargetRenderer {
        override fun renderUseSiteTarget(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {}
    }

    public object WITH_USES_SITE : KaAnnotationUseSiteTargetRenderer {
        override fun renderUseSiteTarget(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            val useSite = annotation.useSiteTarget ?: return
            printer.append(useSite.renderName)
            printer.append(':')
        }
    }

    public object WITH_NON_DEFAULT_USE_SITE : KaAnnotationUseSiteTargetRenderer {
        override fun renderUseSiteTarget(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            val print = when (owner) {
                is KaReceiverParameterSymbol -> true
                !is KaCallableSymbol -> return
                is KaAnonymousFunctionSymbol -> true
                is KaConstructorSymbol -> true
                is KaFunctionSymbol -> true
                is KaPropertyGetterSymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_GETTER
                is KaPropertySetterSymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.PROPERTY_SETTER
                is KaSamConstructorSymbol -> true
                is KaBackingFieldSymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.FIELD
                is KaEnumEntrySymbol -> true
                is KaValueParameterSymbol -> {
                    val containingSymbol = with(analysisSession) { owner.containingSymbol }
                    containingSymbol !is KaPropertySetterSymbol || annotation.useSiteTarget != AnnotationUseSiteTarget.SETTER_PARAMETER
                }
                is KaJavaFieldSymbol -> true
                is KaLocalVariableSymbol -> true
                is KaPropertySymbol -> annotation.useSiteTarget != AnnotationUseSiteTarget.PROPERTY
                else -> return
            }

            if (print) {
                WITH_USES_SITE.renderUseSiteTarget(analysisSession, annotation, owner, annotationRenderer, printer)
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaAnnotationUseSiteTargetRenderer' instead", ReplaceWith("KaAnnotationUseSiteTargetRenderer"))
public typealias KtAnnotationUseSiteTargetRenderer = KaAnnotationUseSiteTargetRenderer