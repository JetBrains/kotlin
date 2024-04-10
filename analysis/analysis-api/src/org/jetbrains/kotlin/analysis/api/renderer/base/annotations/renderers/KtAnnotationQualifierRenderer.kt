/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

public interface KtAnnotationQualifierRenderer {
    public fun renderQualifier(
        analysisSession: KtAnalysisSession,
        annotation: KtAnnotationApplication,
        owner: KtAnnotated,
        annotationRenderer: KtAnnotationRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_QUALIFIED_NAMES : KtAnnotationQualifierRenderer {
        override fun renderQualifier(
            analysisSession: KtAnalysisSession,
            annotation: KtAnnotationApplication,
            owner: KtAnnotated,
            annotationRenderer: KtAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val classId = annotation.classId
                if (classId != null) {
                    append(classId.asSingleFqName().render())
                } else {
                    append("ERROR_ANNOTATION")
                }
            }
        }
    }

    public object WITH_SHORT_NAMES : KtAnnotationQualifierRenderer {
        override fun renderQualifier(
            analysisSession: KtAnalysisSession,
            annotation: KtAnnotationApplication,
            owner: KtAnnotated,
            annotationRenderer: KtAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val classId = annotation.classId
                if (classId != null) {
                    printer.append(classId.shortClassName.render())
                } else {
                    printer.append("ERROR_ANNOTATION")
                }
            }
        }
    }
}