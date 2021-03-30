/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixAction
import com.intellij.codeInsight.intention.EmptyIntentionAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.colors.CodeInsightColors
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
        holder: HighlightInfoHolder,
        diagnostics: Collection<Diagnostic>,
        highlightInfoBuilderByDiagnostic: MutableMap<Diagnostic, HighlightInfo>? = null,
        highlightInfoByTextRange: MutableMap<TextRange, HighlightInfo>?,
        fixesMap: MultiMap<Diagnostic, IntentionAction>?,
        calculatingInProgress: Boolean
    ) {
        for (range in ranges) {
            for (diagnostic in diagnostics) {
                create(diagnostic, range) { info ->
                    holder.add(info)
                    highlightInfoBuilderByDiagnostic?.put(diagnostic, info)
                    if (fixesMap != null) {
                        applyFixes(fixesMap, diagnostic, info)
                    }
                    if (calculatingInProgress && highlightInfoByTextRange?.containsKey(range) == false) {
                        highlightInfoByTextRange[range] = info
                        QuickFixAction.registerQuickFixAction(info, CalculatingIntentionAction())
                    }
                }
            }
        }
    }

    internal fun applyFixes(
        fixesMap: MultiMap<Diagnostic, IntentionAction>,
        diagnostic: Diagnostic,
        info: HighlightInfo
    ) {
        val fixes = fixesMap[diagnostic]
        fixes.filter { it is IntentionAction || it is KotlinUniversalQuickFix }.forEach {
            QuickFixAction.registerQuickFixAction(info, it)
        }

        if (diagnostic.severity == Severity.WARNING) {
            if (fixes.isEmpty()) {
                // if there are no quick fixes we need to register an EmptyIntentionAction to enable 'suppress' actions
                QuickFixAction.registerQuickFixAction(info, EmptyIntentionAction(diagnostic.factory.name))
            }
        }
    }

    private fun create(diagnostic: Diagnostic, range: TextRange, consumer: (HighlightInfo) -> Unit) {
        val message = nonDefaultMessage ?: getDefaultMessage(diagnostic)
        HighlightInfo
            .newHighlightInfo(toHighlightInfoType(highlightType, diagnostic.severity))
            .range(range)
            .description(message)
            .escapedToolTip(getMessage(diagnostic))
            .also { builder -> textAttributes?.let(builder::textAttributes) ?: convertSeverityTextAttributes(highlightType, diagnostic.severity)?.let(builder::textAttributes) }
            .also {
                if (diagnostic.severity == Severity.WARNING) {
                    it.problemGroup(KotlinSuppressableWarningProblemGroup(diagnostic.factory))
                }
            }
            .createUnconditionally()
            .also(consumer)
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

    private fun toHighlightInfoType(highlightType: ProblemHighlightType?, severity: Severity): HighlightInfoType =
        when (highlightType) {
            ProblemHighlightType.LIKE_UNUSED_SYMBOL -> HighlightInfoType.UNUSED_SYMBOL
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL -> HighlightInfoType.WRONG_REF
            ProblemHighlightType.LIKE_DEPRECATED -> HighlightInfoType.DEPRECATED
            ProblemHighlightType.LIKE_MARKED_FOR_REMOVAL -> HighlightInfoType.MARKED_FOR_REMOVAL
            else -> convertSeverity(highlightType, severity)
        }

    private fun convertSeverity(highlightType: ProblemHighlightType?, severity: Severity): HighlightInfoType =
        when (severity) {
            Severity.ERROR -> HighlightInfoType.ERROR
            Severity.WARNING -> {
                if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                    HighlightInfoType.WEAK_WARNING
                } else HighlightInfoType.WARNING
            }
            Severity.INFO -> HighlightInfoType.WEAK_WARNING
            else -> HighlightInfoType.INFORMATION
        }

    private fun convertSeverityTextAttributes(highlightType: ProblemHighlightType?, severity: Severity): TextAttributesKey? =
        when (highlightType) {
            null, ProblemHighlightType.GENERIC_ERROR_OR_WARNING ->
                when (severity) {
                    Severity.ERROR -> CodeInsightColors.ERRORS_ATTRIBUTES
                    Severity.WARNING -> {
                        if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                            CodeInsightColors.WEAK_WARNING_ATTRIBUTES
                        } else CodeInsightColors.WARNINGS_ATTRIBUTES
                    }
                    Severity.INFO -> CodeInsightColors.WARNINGS_ATTRIBUTES
                    else -> null
                }
            ProblemHighlightType.GENERIC_ERROR -> CodeInsightColors.ERRORS_ATTRIBUTES
            else -> null
        }

}
