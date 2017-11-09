/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AddSuspendModifierFix(element: KtModifierListOwner, private val name: String?): AddModifierFix(element, KtTokens.SUSPEND_KEYWORD) {

    override fun getText() =
            when (element) {
                is KtNamedFunction -> "Make ${name ?: "containing function"} suspend"
                is KtTypeReference -> "Make ${name ?: "receiver"} type suspend"
                else -> super.getText()
            }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement
            val function = (element as? KtElement)?.containingDeclarationForPseudocode as? KtNamedFunction ?: return null

            return AddSuspendModifierFix(function, function.name)
        }
    }

    object UnresolvedReferenceFactory : KotlinSingleIntentionActionFactory() {

        private val suspendExtensionNames = setOf("startCoroutine", "createCoroutine")

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val refExpr = diagnostic.psiElement as? KtNameReferenceExpression ?: return null
            if (refExpr.getReferencedName() !in suspendExtensionNames) return null

            val callParent = refExpr.parent as? KtCallExpression ?: return null
            val qualifiedGrandParent = callParent.parent as? KtQualifiedExpression ?: return null
            if (callParent !== qualifiedGrandParent.selectorExpression || refExpr !== callParent.calleeExpression) return null
            val receiver = qualifiedGrandParent.receiverExpression as? KtNameReferenceExpression ?: return null

            val context = receiver.analyze(BodyResolveMode.PARTIAL)
            val receiverDescriptor = context[BindingContext.REFERENCE_TARGET, receiver] as? ValueDescriptor ?: return null
            if (!receiverDescriptor.type.isFunctionType) return null
            val declaration = DescriptorToSourceUtils.descriptorToDeclaration(receiverDescriptor) as? KtCallableDeclaration
                              ?: return null
            if (declaration is KtFunction) return null
            val variableTypeReference = declaration.typeReference ?: return null

            return AddSuspendModifierFix(variableTypeReference, declaration.name)
        }
    }
}