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
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
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

    private fun create(diagnostic: Diagnostic, range: TextRange, holder: AnnotationHolder): Annotation =
        Diagnostic2Annotation.createAnnotation(
            diagnostic,
            range,
            holder,
            nonDefaultMessage,
            textAttributes,
            highlightType,
            IdeErrorMessages::render
        )
}
