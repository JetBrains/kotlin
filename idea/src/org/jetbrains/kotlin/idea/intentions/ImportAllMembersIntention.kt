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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.conversion.copy.range
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.isInImportDirective
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier

class ImportAllMembersIntention : SelfTargetingIntention<KtDotQualifiedExpression>(
        KtDotQualifiedExpression::class.java,
        "Import members with '*'"
){
    override fun isApplicableTo(element: KtDotQualifiedExpression, caretOffset: Int): Boolean {
        if (!element.receiverExpression.range.containsOffset(caretOffset)) return false

        val target = target(element) ?: return false
        val targetFqName = target.importableFqName ?: return false

        if (element.isInImportDirective()) return false

        val file = element.containingKtFile
        val project = file.project
        val dummyFileText = (file.packageDirective?.text ?: "") + "\n" + (file.importList?.text ?: "")
        val dummyFile = KtPsiFactory(project).createAnalyzableFile("Dummy.kt", dummyFileText, file)
        val helper = ImportInsertHelper.getInstance(project)
        if (helper.importDescriptor(dummyFile, target, forceAllUnderImport = true) == ImportDescriptorResult.FAIL) return false

        text = "Import members from '${targetFqName.parent().asString()}'"
        return true
    }

    override fun applyTo(element: KtDotQualifiedExpression, editor: Editor?) {
        element.importReceiverMembers()
    }

    companion object {
        fun KtDotQualifiedExpression.importReceiverMembers() {
            val target = target(this)!!
            val classFqName = target.importableFqName!!.parent()

            ImportInsertHelper.getInstance(project).importDescriptor(containingKtFile, target, forceAllUnderImport = true)

            val qualifiedExpressions = containingKtFile.collectDescendantsOfType<KtDotQualifiedExpression> { qualifiedExpression ->
                val qualifierName = qualifiedExpression.receiverExpression.getQualifiedElementSelector() as? KtNameReferenceExpression
                qualifierName?.getReferencedNameAsName() == classFqName.shortName() && target(qualifiedExpression)?.importableFqName?.parent() == classFqName
            }

            //TODO: not deep
            ShortenReferences.DEFAULT.process(qualifiedExpressions)
        }

        private fun target(expression: KtDotQualifiedExpression): DeclarationDescriptor? {
            val bindingContext = expression.analyze(BodyResolveMode.PARTIAL)
            if (bindingContext[BindingContext.QUALIFIER, expression.receiverExpression] !is ClassQualifier) {
                return null
            }
            val selector = expression.getQualifiedElementSelector() as? KtNameReferenceExpression ?: return null
            return selector.mainReference.resolveToDescriptors(bindingContext).firstOrNull()
        }
    }

}