/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.moveCaret
import org.jetbrains.kotlin.idea.util.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode


class InsertDelegationCallQuickfix(val isThis: Boolean, element: KtSecondaryConstructor) :
    KotlinQuickFixAction<KtSecondaryConstructor>(element) {
    override fun getText() = KotlinBundle.message("insert.delegation.call", keywordToUse)
    override fun getFamilyName() = "Insert explicit delegation call"

    private val keywordToUse = if (isThis) "this" else "super"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val newDelegationCall = element.replaceImplicitDelegationCallWithExplicit(isThis)

        val resolvedCall = newDelegationCall.resolveToCall(BodyResolveMode.FULL)
        val descriptor = element.unsafeResolveToDescriptor()

        // if empty call is ok and it's resolved to another constructor, do not move caret
        if (resolvedCall?.isReallySuccess() == true && resolvedCall.candidateDescriptor.original != descriptor) return

        val leftParOffset = newDelegationCall.valueArgumentList!!.leftParenthesis!!.textOffset

        editor?.moveCaret(leftParOffset + 1)
    }

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.hasImplicitDelegationCall()
    }

    object InsertThisDelegationCallFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) =
            diagnostic.createIntentionForFirstParentOfType<KtSecondaryConstructor> { secondaryConstructor ->
                return if (secondaryConstructor.getContainingClassOrObject().getConstructorsCount() <= 1 ||
                    !secondaryConstructor.hasImplicitDelegationCall()
                ) null
                else
                    InsertDelegationCallQuickfix(isThis = true, element = secondaryConstructor)
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
