/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.util.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.jvm.checkers.JavaAnnotationCallChecker

class ReplaceJavaAnnotationPositionedArgumentsFix(element: KtAnnotationEntry) : KotlinQuickFixAction<KtAnnotationEntry>(element),
    CleanupFix {
    override fun getText(): String = "Replace invalid positioned arguments for annotation"
    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val resolvedCall = element.resolveToCall() ?: return
        val psiFactory = KtPsiFactory(project)

        for ((key, value) in JavaAnnotationCallChecker.getJavaAnnotationCallValueArgumentsThatShouldBeNamed(resolvedCall)) {
            val valueArgument = (value as? ExpressionValueArgument)?.valueArgument ?: continue
            val expression = valueArgument.getArgumentExpression() ?: continue

            valueArgument.asElement().replace(psiFactory.createArgument(expression, key.name))
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
            diagnostic.createIntentionForFirstParentOfType(::ReplaceJavaAnnotationPositionedArgumentsFix)
    }
}
