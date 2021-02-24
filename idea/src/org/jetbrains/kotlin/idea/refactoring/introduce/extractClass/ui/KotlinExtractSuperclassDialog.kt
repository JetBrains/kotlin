/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ui

import com.intellij.psi.PsiElement
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ExtractSuperInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.KotlinExtractSuperclassHandler
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.extractClassMembers
import org.jetbrains.kotlin.idea.refactoring.pullUp.getInterfaceContainmentVerifier
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

class KotlinExtractSuperclassDialog(
    originalClass: KtClassOrObject,
    targetParent: PsiElement,
    conflictChecker: (KotlinExtractSuperDialogBase) -> Boolean,
    refactoring: (ExtractSuperInfo) -> Unit
) : KotlinExtractSuperDialogBase(
    originalClass,
    targetParent,
    conflictChecker,
    false,
    KotlinExtractSuperclassHandler.REFACTORING_NAME,
    refactoring
) {
    companion object {
        private const val DESTINATION_PACKAGE_RECENT_KEY = "KotlinExtractSuperclassDialog.RECENT_KEYS"
    }

    init {
        init()
    }

    override fun createMemberInfoModel(): MemberInfoModelBase {
        return object : MemberInfoModelBase(
            originalClass,
            extractClassMembers(originalClass),
            getInterfaceContainmentVerifier { selectedMembers }
        ) {
            override fun isAbstractEnabled(memberInfo: KotlinMemberInfo): Boolean {
                if (!super.isAbstractEnabled(memberInfo)) return false
                val member = memberInfo.member
                return member is KtNamedFunction || member is KtProperty || member is KtParameter
            }
        }
    }

    override fun getDestinationPackageRecentKey() = DESTINATION_PACKAGE_RECENT_KEY

    override fun getClassNameLabelText(): String = RefactoringBundle.message("superclass.name")

    override fun getPackageNameLabelText(): String = RefactoringBundle.message("package.for.new.superclass")

    override fun getEntityName(): String = RefactoringBundle.message("ExtractSuperClass.superclass")

    override fun getTopLabelText(): String = RefactoringBundle.message("extract.superclass.from")

    override fun getDocCommentPolicySetting() = KotlinRefactoringSettings.instance.EXTRACT_SUPERCLASS_JAVADOC

    override fun setDocCommentPolicySetting(policy: Int) {
        KotlinRefactoringSettings.instance.EXTRACT_SUPERCLASS_JAVADOC = policy
    }

    override fun getExtractedSuperNameNotSpecifiedMessage(): String = RefactoringBundle.message("no.superclass.name.specified")

    override fun getHelpId() = HelpID.EXTRACT_SUPERCLASS
}