/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.test.cases.annotations

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.symbols.KaDebugRenderer
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.analysis.test.framework.utils.indented
import org.jetbrains.kotlin.name.ClassId

object TestAnnotationRenderer {
    context(session: KaSession)
    fun renderAnnotations(annotations: KaAnnotationList, prefix: String = "annotations: ") = buildString {
        renderAnnotationsRecursive(annotations, currentMetaAnnotations = null, indent = 0, prefix)
    }

    context(session: KaSession)
    fun renderAnnotationsWithMeta(annotations: KaAnnotationList) = buildString {
        renderAnnotationsRecursive(annotations, currentMetaAnnotations = setOf(), indent = 0)
    }

    context(session: KaSession)
    private fun StringBuilder.renderAnnotationsRecursive(
        annotations: KaAnnotationList,
        currentMetaAnnotations: Set<ClassId>?,
        indent: Int,
        prefix: String = "annotations: ",
    ) {
        appendLine("$prefix[".indented(indent))
        for (annotation in annotations) {
            appendLine(KaDebugRenderer().renderAnnotationApplication(annotation).indented(indent = indent + 2))
            if (currentMetaAnnotations != null) {
                val classId = annotation.classId ?: continue
                if (classId in currentMetaAnnotations) {
                    appendLine("<recursive meta-annotation ${classId}>".indented(indent + 4))
                    continue
                }

                val metaAnnotations = findClass(classId)?.annotations
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
