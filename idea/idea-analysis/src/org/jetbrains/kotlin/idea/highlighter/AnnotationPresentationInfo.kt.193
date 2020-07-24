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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.util.containers.MultiMap
import com.intellij.xml.util.XmlStringUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.inspections.KotlinUniversalQuickFix

class AnnotationPresentationInfo(
    val ranges: List<TextRange>,
    val nonDefaultMessage: String? = null,
    val highlightType: ProblemHighlightType? = null,
    val textAttributes: TextAttributesKey? = null
) {

    fun processDiagnostics(holder: AnnotationHolder, diagnostics: List<Diagnostic>, fixesMap: MultiMap<Diagnostic, IntentionAction>) {
        for (range in ranges) {
            for (diagnostic in diagnostics) {
                val fixes = fixesMap[diagnostic]
                val annotation = create(diagnostic, range, holder)

                fixes.forEach {
                    when (it) {
                        is KotlinUniversalQuickFix -> annotation.registerUniversalFix(it, null, null)
                        is IntentionAction -> annotation.registerFix(it)
                    }
                }

                if (diagnostic.severity == Severity.WARNING) {
                    annotation.problemGroup = KotlinSuppressableWarningProblemGroup(diagnostic.factory)

                    if (fixes.isEmpty()) {
                        // if there are no quick fixes we need to register an EmptyIntentionAction to enable 'suppress' actions
                        annotation.registerFix(EmptyIntentionAction(diagnostic.factory.name))
                    }
                }
            }
        }
    }

    private fun create(diagnostic: Diagnostic, range: TextRange, holder: AnnotationHolder): Annotation {
        val defaultMessage = nonDefaultMessage ?: getDefaultMessage(diagnostic)

        val annotation = when (diagnostic.severity) {
            Severity.ERROR -> holder.createErrorAnnotation(range, defaultMessage)
            Severity.WARNING -> {
                if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                    holder.createWeakWarningAnnotation(range, defaultMessage)
                } else {
                    holder.createWarningAnnotation(range, defaultMessage)
                }
            }
            Severity.INFO -> holder.createInfoAnnotation(range, defaultMessage)
        }

        annotation.tooltip = getMessage(diagnostic)

        if (highlightType != null) {
            annotation.highlightType = highlightType
        }

        if (textAttributes != null) {
            annotation.textAttributes = textAttributes
        }

        return annotation
    }

    private fun getMessage(diagnostic: Diagnostic): String {
        var message = IdeErrorMessages.render(diagnostic)
        if (ApplicationManager.getApplication().isInternal || ApplicationManager.getApplication().isUnitTestMode) {
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
        if (ApplicationManager.getApplication().isInternal || ApplicationManager.getApplication().isUnitTestMode) {
            return "[${diagnostic.factory.name}] $message"
        }
        return message
    }
}
