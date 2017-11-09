/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        private val isObjectCall: Boolean) : KotlinQuickFixAction<KtCallExpression>(element) {

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
