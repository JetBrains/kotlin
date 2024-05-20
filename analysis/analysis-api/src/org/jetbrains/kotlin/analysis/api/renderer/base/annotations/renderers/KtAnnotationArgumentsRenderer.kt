/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValueRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KaAnnotationRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

public interface KaAnnotationArgumentsRenderer {
    public fun renderAnnotationArguments(
        analysisSession: KaSession,
        annotation: KaAnnotationApplication,
        owner: KaAnnotated,
        annotationRenderer: KaAnnotationRenderer,
        printer: PrettyPrinter,
    )

    public object NONE : KaAnnotationArgumentsRenderer {
        override fun renderAnnotationArguments(
            analysisSession: KaSession,
            annotation: KaAnnotationApplication,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {}
    }

    public object IF_ANY : KaAnnotationArgumentsRenderer {
        override fun renderAnnotationArguments(
            analysisSession: KaSession,
            annotation: KaAnnotationApplication,
            owner: KaAnnotated,
            annotationRenderer: KaAnnotationRenderer,
            printer: PrettyPrinter,
        ) {
            if (annotation !is KaAnnotationApplicationWithArgumentsInfo) return

            if (annotation.arguments.isEmpty()) return
            printer.printCollection(annotation.arguments, prefix = "(", postfix = ")") { argument ->
                append(argument.name.render())
                append(" = ")
                append(KaAnnotationValueRenderer.render(argument.expression))
            }
        }
    }
}

public typealias KtAnnotationArgumentsRenderer = KaAnnotationArgumentsRenderer