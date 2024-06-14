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
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

@KaExperimentalApi
public interface KaAnnotationQualifierRenderer {
    public fun renderQualifier(
        analysisSession: KaSession,
        annotation: KaAnnotation,
        owner: KaAnnotated,
        annotationRenderer: KaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_QUALIFIED_NAMES : KaAnnotationQualifierRenderer {
        override fun renderQualifier(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
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

    public object WITH_SHORT_NAMES : KaAnnotationQualifierRenderer {
        override fun renderQualifier(
            analysisSession: KaSession,
            annotation: KaAnnotation,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
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

@KaExperimentalApi
@Deprecated("Use 'KaAnnotationQualifierRenderer' instead", ReplaceWith("KaAnnotationQualifierRenderer"))
public typealias KtAnnotationQualifierRenderer = KaAnnotationQualifierRenderer