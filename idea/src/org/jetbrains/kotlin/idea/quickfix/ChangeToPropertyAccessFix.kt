/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject

class ChangeToPropertyAccessFix(
    element: KtCallExpression,
    private val isObjectCall: Boolean
) : KotlinQuickFixAction<KtCallExpression>(element) {

    override fun getFamilyName() = if (isObjectCall) "Remove invocation" else "Change to property access"

    override fun getText() = familyName

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        element.replace(element.calleeExpression as KtExpression)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtCallExpression>? {
            val expression = diagnostic.psiElement.parent as? KtCallExpression ?: return null
            if (expression.valueArguments.isEmpty()) {
                val isObjectCall = expression.calleeExpression?.getCallableDescriptor() is FakeCallableDescriptorForObject
                return ChangeToPropertyAccessFix(expression, isObjectCall)
            }
            return null
        }
    }
}
