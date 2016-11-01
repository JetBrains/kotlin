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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddTypeToLHSOfCallableReferenceFix(
        expression: KtCallableReferenceExpression
) : KotlinQuickFixAction<KtCallableReferenceExpression>(expression), CleanupFix {
    override fun getFamilyName() = "Add type to left-hand side"
    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val resolvedCall = element.callableReference.getResolvedCall(element.analyze(BodyResolveMode.PARTIAL)) ?: return
        val receiver = with(resolvedCall) {
            dispatchReceiver ?: extensionReceiver ?: return
        }
        val expression = KtPsiFactory(project).createExpression(IdeDescriptorRenderers.SOURCE_CODE.renderType(receiver.type))
        element.setReceiverExpression(expression)
        ShortenReferences.DEFAULT.process(element)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            return AddTypeToLHSOfCallableReferenceFix(diagnostic.psiElement.parent as KtCallableReferenceExpression)
        }
    }
}
