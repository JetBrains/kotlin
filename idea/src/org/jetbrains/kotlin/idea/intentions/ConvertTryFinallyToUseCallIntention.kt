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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.contentRange
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.typeUtil.supertypes

class ConvertTryFinallyToUseCallInspection : IntentionBasedInspection<KtTryExpression>(ConvertTryFinallyToUseCallIntention::class) {
    override fun inspectionTarget(element: KtTryExpression) = element.tryKeyword ?: element.tryBlock
}

class ConvertTryFinallyToUseCallIntention : SelfTargetingOffsetIndependentIntention<KtTryExpression>(
        KtTryExpression::class.java, "Convert try-finally to .use()"
) {
    override fun applyTo(element: KtTryExpression, editor: Editor?) {
        val finallySection = element.finallyBlock!!
        val finallyDotCall = finallySection.finalExpression.statements.singleOrNull() as KtDotQualifiedExpression
        val resourceReference = finallyDotCall.receiverExpression as KtNameReferenceExpression

        val factory = KtPsiFactory(element)

        val useCallExpression = factory.buildExpression {
            appendName(resourceReference.getReferencedNameAsName())
            appendFixedText(".use {")

            appendName(resourceReference.getReferencedNameAsName())
            appendFixedText("->")

            appendChildRange(element.tryBlock.contentRange())
            appendFixedText("}")
        }

        element.replace(useCallExpression)
    }

    override fun isApplicableTo(element: KtTryExpression): Boolean {
        // Single statement in finally, no catch blocks
        val finallySection = element.finallyBlock ?: return false
        val finallyDotCall = finallySection.finalExpression.statements.singleOrNull() as? KtDotQualifiedExpression ?: return false
        if (element.catchClauses.isNotEmpty()) return false

        // Like resource.close()
        val resourceReference = finallyDotCall.receiverExpression as? KtNameReferenceExpression ?: return false
        val resourceCall = finallyDotCall.selectorExpression as? KtCallExpression ?: return false
        if (resourceCall.calleeExpression?.text != "close") return false

        // resource is Closeable immutable local variable
        val resourceDescriptor =
                element.analyze().get(BindingContext.REFERENCE_TARGET, resourceReference) as? VariableDescriptor ?: return false
        return !resourceDescriptor.isVar && resourceDescriptor.type.supertypes().any {
            it.constructor.declarationDescriptor?.fqNameSafe?.asString().let {
                it == "java.io.Closeable" || it == "java.lang.AutoCloseable"
            }
        }
    }
}