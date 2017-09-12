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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess


class InsertDelegationCallQuickfix(val isThis: Boolean, element: KtSecondaryConstructor) : KotlinQuickFixAction<KtSecondaryConstructor>(element) {
    override fun getText() = KotlinBundle.message("insert.delegation.call", keywordToUse)
    override fun getFamilyName() = "Insert explicit delegation call"

    private val keywordToUse = if (isThis) "this" else "super"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val newDelegationCall = element.replaceImplicitDelegationCallWithExplicit(isThis)

        val resolvedCall = newDelegationCall.getResolvedCall(newDelegationCall.analyze())
        val descriptor = element.unsafeResolveToDescriptor()

        // if empty call is ok and it's resolved to another constructor, do not move caret
        if (resolvedCall?.isReallySuccess() ?: false && resolvedCall!!.candidateDescriptor.original != descriptor) return

        val leftParOffset = newDelegationCall.valueArgumentList!!.leftParenthesis!!.textOffset

        editor?.moveCaret(leftParOffset + 1)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file) && element.hasImplicitDelegationCall()
    }

    object InsertThisDelegationCallFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = diagnostic.createIntentionForFirstParentOfType<KtSecondaryConstructor> {
            secondaryConstructor ->
            if (secondaryConstructor.getContainingClassOrObject().getConstructorsCount() <= 1 ||
                !secondaryConstructor.hasImplicitDelegationCall()) return null

            return InsertDelegationCallQuickfix(isThis = true, element = secondaryConstructor)
        }

        private fun KtClassOrObject.getConstructorsCount() = (descriptor as ClassDescriptor).constructors.size
    }

    object InsertSuperDelegationCallFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val secondaryConstructor = diagnostic.psiElement.getNonStrictParentOfType<KtSecondaryConstructor>() ?: return null
            if (!secondaryConstructor.hasImplicitDelegationCall()) return null
            val klass = secondaryConstructor.getContainingClassOrObject() as? KtClass ?: return null
            if (klass.hasPrimaryConstructor()) return null

            return InsertDelegationCallQuickfix(isThis = false, element = secondaryConstructor)
        }
    }
}
