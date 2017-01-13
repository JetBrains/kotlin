/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ImportMemberIntention : SelfTargetingOffsetIndependentIntention<KtNameReferenceExpression>(
        KtNameReferenceExpression::class.java,
        "Add import for member"
){
    override fun isApplicableTo(element: KtNameReferenceExpression): Boolean {
        val qualifiedElement = element.getQualifiedElement()
        if (qualifiedElement == element) return false

        if (element.isInImportDirective()) return false

        val fqName = targetFqName(qualifiedElement) ?: return false

        text = "Add import for '${fqName.asString()}'"
        return true
    }

    override fun applyTo(element: KtNameReferenceExpression, editor: Editor?) {
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        val targets = element.mainReference.resolveToDescriptors(bindingContext)
        val fqName = targets.map { it.importableFqName!! }.single()

        val file = element.containingKtFile
        val helper = ImportInsertHelper.getInstance(element.project)
        if (helper.importDescriptor(file, targets.first()) == ImportDescriptorResult.FAIL) return

        val qualifiedExpressions = file.collectDescendantsOfType<KtDotQualifiedExpression> { qualifiedExpression ->
            val selector = qualifiedExpression.getQualifiedElementSelector() as? KtNameReferenceExpression
            selector?.getReferencedNameAsName() == fqName.shortName() && targetFqName(qualifiedExpression) == fqName
        }
        val userTypes = file.collectDescendantsOfType<KtUserType> { userType ->
            val selector = userType.getQualifiedElementSelector() as? KtNameReferenceExpression
            selector?.getReferencedNameAsName() == fqName.shortName() && targetFqName(userType) == fqName
        }

        //TODO: not deep
        ShortenReferences.DEFAULT.process(qualifiedExpressions + userTypes)
    }

    private fun targetFqName(qualifiedElement: KtElement): FqName? {
        val nameExpression = qualifiedElement.getQualifiedElementSelector() as? KtNameReferenceExpression ?: return null
        val receiver = nameExpression.getReceiverExpression() ?: return null
        val bindingContext = qualifiedElement.analyze(BodyResolveMode.PARTIAL)
        if (bindingContext[BindingContext.QUALIFIER, receiver] == null) return null

        val targets = nameExpression.mainReference.resolveToDescriptors(bindingContext)
        if (targets.isEmpty()) return null
        if (!targets.all { it.canBeReferencedViaImport() }) return null
        return targets.map { it.importableFqName }.singleOrNull()
    }
}