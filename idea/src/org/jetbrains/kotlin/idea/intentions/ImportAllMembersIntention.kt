/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.resolveToDescriptors
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.psiUtil.isInImportDirective
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier

class ImportAllMembersIntention : SelfTargetingIntention<KtElement>(
    KtElement::class.java,
    KotlinBundle.lazyMessage("import.members.with")
), HighPriorityAction {
    override fun isApplicableTo(element: KtElement, caretOffset: Int): Boolean {
        val receiverExpression = element.receiverExpression() ?: return false
        if (!receiverExpression.range.containsOffset(caretOffset)) return false

        val target = target(element, receiverExpression) ?: return false
        val targetFqName = target.importableFqName ?: return false

        if (receiverExpression.isInImportDirective()) return false

        val file = element.containingKtFile
        val project = file.project
        val dummyFileText = (file.packageDirective?.text ?: "") + "\n" + (file.importList?.text ?: "")
        val dummyFile = KtPsiFactory(project).createAnalyzableFile("Dummy.kt", dummyFileText, file)
        val helper = ImportInsertHelper.getInstance(project)
        if (helper.importDescriptor(dummyFile, target, forceAllUnderImport = true) == ImportDescriptorResult.FAIL) return false

        setTextGetter(KotlinBundle.lazyMessage("import.members.from.0", targetFqName.parent().asString()))
        return true
    }

    override fun applyTo(element: KtElement, editor: Editor?) = element.importReceiverMembers()

    companion object {
        fun KtElement.importReceiverMembers() {
            val target = target(this) ?: return
            val classFqName = target.importableFqName!!.parent()

            ImportInsertHelper.getInstance(project).importDescriptor(containingKtFile, target, forceAllUnderImport = true)
            val qualifiedExpressions = containingKtFile.collectDescendantsOfType<KtDotQualifiedExpression> { qualifiedExpression ->
                val qualifierName = qualifiedExpression.receiverExpression.getQualifiedElementSelector() as? KtNameReferenceExpression
                qualifierName?.getReferencedNameAsName() == classFqName.shortName() && target(qualifiedExpression)?.importableFqName
                    ?.parent() == classFqName
            }

            val userTypes = containingKtFile.collectDescendantsOfType<KtUserType> { userType ->
                val receiver = userType.receiverExpression()?.getQualifiedElementSelector() as? KtNameReferenceExpression
                receiver?.getReferencedNameAsName() == classFqName.shortName() && target(userType)?.importableFqName
                    ?.parent() == classFqName
            }

            //TODO: not deep
            ShortenReferences.DEFAULT.process(qualifiedExpressions + userTypes)
        }

        private fun target(qualifiedElement: KtElement, receiverExpression: KtExpression): DeclarationDescriptor? {
            val bindingContext = qualifiedElement.analyze(BodyResolveMode.PARTIAL)
            if (bindingContext[BindingContext.QUALIFIER, receiverExpression] !is ClassQualifier) {
                return null
            }

            val selector = qualifiedElement.getQualifiedElementSelector() as? KtNameReferenceExpression ?: return null
            return selector.mainReference.resolveToDescriptors(bindingContext).firstOrNull()
        }

        private fun target(qualifiedElement: KtElement): DeclarationDescriptor? {
            val receiverExpression = qualifiedElement.receiverExpression() ?: return null
            return target(qualifiedElement, receiverExpression)
        }

        private fun KtElement.receiverExpression(): KtExpression? = when (this) {
            is KtDotQualifiedExpression -> receiverExpression
            is KtUserType -> qualifier?.referenceExpression
            else -> null
        }
    }

}