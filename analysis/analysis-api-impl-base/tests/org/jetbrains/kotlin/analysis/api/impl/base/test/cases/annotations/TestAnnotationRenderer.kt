/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.KtAnnotationsList
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.utils.indented
import org.jetbrains.kotlin.name.ClassId

object TestAnnotationRenderer {
    context (KtAnalysisSession)
    fun renderAnnotations(annotations: KtAnnotationsList) = buildString {
        renderAnnotationsRecursive(annotations, currentMetaAnnotations = null, indent = 0)
    }

    context(KtAnalysisSession)
    fun renderAnnotationsWithMeta(annotations: KtAnnotationsList) = buildString {
        renderAnnotationsRecursive(annotations, currentMetaAnnotations = setOf(), indent = 0)
    }

    context(KtAnalysisSession)
    private fun StringBuilder.renderAnnotationsRecursive(
        annotations: KtAnnotationsList,
        currentMetaAnnotations: Set<ClassId>?,
        indent: Int
    ) {
        appendLine("annotations: [".indented(indent))
        for (annotation in annotations.annotations) {
            appendLine(DebugSymbolRenderer().renderAnnotationApplication(annotation).indented(indent = indent + 2))
            if (currentMetaAnnotations != null) {
                val classId = annotation.classId ?: continue
                if (classId in currentMetaAnnotations) {
                    appendLine("<recursive meta-annotation ${classId}>".indented(indent + 4))
                    continue
                }

                val metaAnnotations = getClassOrObjectSymbolByClassId(classId)?.annotationsList
                if (metaAnnotations != null) {
                    renderAnnotationsRecursive(metaAnnotations, currentMetaAnnotations + classId, indent = indent + 4)
                } else {
                    appendLine("<unknown meta-annotation ${classId}>".indented(indent + 4))
                }
            }
        }
        appendLine("]".indented(indent))
    }
}