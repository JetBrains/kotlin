/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.intention.EmptyIntentionAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.util.containers.MultiMap
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.inspections.KotlinUniversalQuickFix
import org.jetbrains.kotlin.idea.util.application.isApplicationInternalMode
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode

class AnnotationPresentationInfo(
    val ranges: List<TextRange>,
    val nonDefaultMessage: String? = null,
    val highlightType: ProblemHighlightType? = null,
    val textAttributes: TextAttributesKey? = null
) {

    fun processDiagnostics(
        holder: AnnotationHolder,
        diagnostics: Collection<Diagnostic>,
        annotationBuilderByDiagnostic: MutableMap<Diagnostic, Annotation>? = null,
        annotationByTextRange: MutableMap<TextRange, Annotation>?,
        fixesMap: MultiMap<Diagnostic, IntentionAction>?,
        calculatingInProgress: Boolean
    ) {
        for (range in ranges) {
            for (diagnostic in diagnostics) {
                create(diagnostic, range, holder) { annotation ->
                    annotationBuilderByDiagnostic?.put(diagnostic, annotation)
                    if (fixesMap != null) {
                        applyFixes(fixesMap, diagnostic, annotation)
                    }
                    if (calculatingInProgress && annotationByTextRange?.containsKey(range) == false) {
                        annotationByTextRange[range] = annotation
                        annotation.registerFix(CalculatingIntentionAction(), range)
                    }
                }
            }
        }
    }

    internal fun applyFixes(
        fixesMap: MultiMap<Diagnostic, IntentionAction>,
        diagnostic: Diagnostic,
        annotation: Annotation
    ) {
        val fixes = fixesMap[diagnostic]
        val textRange = TextRange(annotation.startOffset, annotation.endOffset)
        fixes.forEach {
            when (it) {
                is KotlinUniversalQuickFix -> {
                    annotation.registerBatchFix(it, textRange, null)
                    annotation.registerFix(it, textRange)
                }
                is IntentionAction -> {
                    annotation.registerFix(it, textRange)
                }
            }
        }

        if (diagnostic.severity == Severity.WARNING) {
            if (fixes.isEmpty()) {
                // if there are no quick fixes we need to register an EmptyIntentionAction to enable 'suppress' actions
                //annotation.newFix(EmptyIntentionAction(diagnostic.factory.name)).registerFix()
                annotation.registerFix(EmptyIntentionAction(diagnostic.factory.name), textRange)
            }
        }
    }

    private fun create(diagnostic: Diagnostic, range: TextRange, holder: AnnotationHolder, consumer: (Annotation) -> Unit) {
        val severity = when (diagnostic.severity) {
            Severity.ERROR -> HighlightSeverity.ERROR
            Severity.WARNING -> if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                HighlightSeverity.WEAK_WARNING
            } else HighlightSeverity.WARNING
            Severity.INFO -> HighlightSeverity.WEAK_WARNING
        }


        val message = nonDefaultMessage ?: getDefaultMessage(diagnostic)
        holder.newAnnotation(severity, message)
            .range(range)
            .tooltip(getMessage(diagnostic))
            .also { builder -> highlightType?.let { builder.highlightType(it) } }
            .also { builder -> textAttributes?.let { builder.textAttributes(it) } }
            .also {
                if (diagnostic.severity == Severity.WARNING) {
                    it.problemGroup(KotlinSuppressableWarningProblemGroup(diagnostic.factory))
                }
            }
            .create()
        @Suppress("UNCHECKED_CAST")
        (holder as? List<Annotation>)?.last()?.let(consumer::invoke)
    }

    private fun getMessage(diagnostic: Diagnostic): String {
        var message = IdeErrorMessages.render(diagnostic)
        if (isApplicationInternalMode() || isUnitTestMode()) {
            val factoryName = diagnostic.factory.name
            message = if (message.startsWith("<html>")) {
                "<html>[$factoryName] ${message.substring("<html>".length)}"
            } else {
                "[$factoryName] $message"
            }
        }
        if (!message.startsWith("<html>")) {
            message = "<html><body>${XmlStringUtil.escapeString(message)}</body></html>"
        }
        return message
    }

    private fun getDefaultMessage(diagnostic: Diagnostic): String {
        val message = DefaultErrorMessages.render(diagnostic)
        return if (isApplicationInternalMode() || isUnitTestMode()) {
            "[${diagnostic.factory.name}] $message"
        } else {
            message
        }
    }

}
