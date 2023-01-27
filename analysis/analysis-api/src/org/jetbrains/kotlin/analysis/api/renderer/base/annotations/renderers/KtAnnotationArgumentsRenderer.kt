/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.annotations.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplication
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationApplicationWithArgumentsInfo
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationValueRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.annotations.KtAnnotationRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

public interface KtAnnotationArgumentsRenderer {
    context(KtAnalysisSession, KtAnnotationRenderer)
    public fun renderAnnotationArguments(annotation: KtAnnotationApplication, owner: KtAnnotated, printer: PrettyPrinter)

    public object NONE : KtAnnotationArgumentsRenderer {
        context(KtAnalysisSession, KtAnnotationRenderer)
        override fun renderAnnotationArguments(annotation: KtAnnotationApplication, owner: KtAnnotated, printer: PrettyPrinter) {
        }
    }

    public object IF_ANY : KtAnnotationArgumentsRenderer {
        context(KtAnalysisSession, KtAnnotationRenderer)
        override fun renderAnnotationArguments(
            annotation: KtAnnotationApplication,
            owner: KtAnnotated,
            printer: PrettyPrinter
        ) {
            if (annotation !is KtAnnotationApplicationWithArgumentsInfo) return

            if (annotation.arguments.isEmpty()) return
            printer.printCollection(annotation.arguments, prefix = "(", postfix = ")") { argument ->
                append(argument.name.render())
                append(" = ")
                append(KtAnnotationValueRenderer.render(argument.expression))
            }
        }
    }
}