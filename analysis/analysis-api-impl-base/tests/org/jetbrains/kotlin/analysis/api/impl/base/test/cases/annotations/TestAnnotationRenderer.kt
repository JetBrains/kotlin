/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationsList
import org.jetbrains.kotlin.analysis.api.symbols.DebugSymbolRenderer
import org.jetbrains.kotlin.analysis.test.framework.utils.indented
import org.jetbrains.kotlin.name.ClassId

object TestAnnotationRenderer {
    fun renderAnnotations(analysisSession: KaSession, annotations: KaAnnotationsList) = buildString {
        renderAnnotationsRecursive(analysisSession, annotations, currentMetaAnnotations = null, indent = 0)
    }

    fun renderAnnotationsWithMeta(analysisSession: KaSession, annotations: KaAnnotationsList) = buildString {
        renderAnnotationsRecursive(analysisSession, annotations, currentMetaAnnotations = setOf(), indent = 0)
    }

    private fun StringBuilder.renderAnnotationsRecursive(
        analysisSession: KaSession,
        annotations: KaAnnotationsList,
        currentMetaAnnotations: Set<ClassId>?,
        indent: Int
    ) {
        appendLine("annotations: [".indented(indent))
        for (annotation in annotations.annotations) {
            appendLine(DebugSymbolRenderer().renderAnnotationApplication(analysisSession, annotation).indented(indent = indent + 2))
            if (currentMetaAnnotations != null) {
                val classId = annotation.classId ?: continue
                if (classId in currentMetaAnnotations) {
                    appendLine("<recursive meta-annotation ${classId}>".indented(indent + 4))
                    continue
                }

                val metaAnnotations = with(analysisSession) { getClassOrObjectSymbolByClassId(classId)?.annotationsList }
                if (metaAnnotations != null) {
                    renderAnnotationsRecursive(analysisSession, metaAnnotations, currentMetaAnnotations + classId, indent = indent + 4)
                } else {
                    appendLine("<unknown meta-annotation ${classId}>".indented(indent + 4))
                }
            }
        }
        appendLine("]".indented(indent))
    }
}