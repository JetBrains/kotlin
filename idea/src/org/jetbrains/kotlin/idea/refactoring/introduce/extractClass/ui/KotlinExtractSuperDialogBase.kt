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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ui

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.classMembers.MemberInfoChange
import com.intellij.refactoring.extractSuperclass.JavaExtractSuperBaseDialog
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.RefactoringMessageUtil
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ExtractSuperInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinUsesAndInterfacesDependencyMemberInfoModel
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.awt.BorderLayout
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

abstract class KotlinExtractSuperDialogBase(
        protected val originalClass: KtClassOrObject,
        protected val targetParent: PsiElement,
        private val conflictChecker: (KotlinExtractSuperDialogBase) -> Boolean,
        private val isExtractInterface: Boolean,
        refactoringName: String,
        private val refactoring: (ExtractSuperInfo) -> Unit
) : JavaExtractSuperBaseDialog(originalClass.project, originalClass.toLightClass()!!, emptyList(), refactoringName) {
    private var initComplete: Boolean = false

    private lateinit var memberInfoModel: MemberInfoModelBase

    val selectedMembers: List<KotlinMemberInfo>
        get() = memberInfoModel.memberInfos.filter { it.isChecked }

    private val fileNameField = JTextField()

    open class MemberInfoModelBase(
            originalClass: KtClassOrObject,
            val memberInfos: List<KotlinMemberInfo>,
            interfaceContainmentVerifier: (KtNamedDeclaration) -> Boolean
    ) : KotlinUsesAndInterfacesDependencyMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>(originalClass, null, false, interfaceContainmentVerifier) {
        override fun isFixedAbstract(memberInfo: KotlinMemberInfo?) = true
    }

    val selectedTargetParent: PsiElement
        get() = if (targetParent is PsiDirectory) targetDirectory else targetParent

    val targetFileName: String
        get() = fileNameField.text

    private fun resetFileNameField() {
        if (!initComplete) return
        fileNameField.text = "$extractedSuperName.${KotlinFileType.EXTENSION}"
    }

    protected abstract fun createMemberInfoModel(): MemberInfoModelBase

    override fun getDocCommentPanelName() = "KDoc for abstracts"

    override fun checkConflicts() = conflictChecker(this)

    override fun createActionComponent() = Box.createHorizontalBox()!!

    override fun createDestinationRootPanel() = if (targetParent is PsiDirectory) super.createDestinationRootPanel() else null

    override fun createNorthPanel(): JComponent? {
        return super.createNorthPanel().apply {
            if (targetParent !is PsiDirectory) {
                myPackageNameLabel.parent.remove(myPackageNameLabel)
                myPackageNameField.parent.remove(myPackageNameField)
            }
        }
    }

    override fun createCenterPanel(): JComponent? {
        memberInfoModel = createMemberInfoModel().apply {
            memberInfoChanged(MemberInfoChange(memberInfos))
        }

        return JPanel(BorderLayout()).apply {
            val memberSelectionPanel = KotlinMemberSelectionPanel(
                    RefactoringBundle.message(if (isExtractInterface) "members.to.form.interface" else "members.to.form.superclass"),
                    memberInfoModel.memberInfos,
                    RefactoringBundle.message("make.abstract")
            )
            memberSelectionPanel.table.memberInfoModel = memberInfoModel
            memberSelectionPanel.table.addMemberInfoChangeListener(memberInfoModel)
            add(memberSelectionPanel, BorderLayout.CENTER)

            add(myDocCommentPanel, BorderLayout.EAST)
        }
    }

    override fun init() {
        super.init()

        initComplete = true

        resetFileNameField()
    }

    override fun preparePackage() {
        if (targetParent is PsiDirectory) super.preparePackage()
    }

    override fun isExtractSuperclass() = true

    override fun validateName(name: String): String? {
        return when {
            !KotlinNameSuggester.isIdentifier(name.quoteIfNeeded()) -> RefactoringMessageUtil.getIncorrectIdentifierMessage(name)
            name.unquote() == mySourceClass.name -> "Different name expected"
            else -> null
        }
    }

    override fun createProcessor() = null

    override fun executeRefactoring() {
        val extractInfo = ExtractSuperInfo(
                mySourceClass.unwrapped as KtClassOrObject,
                selectedMembers,
                if (targetParent is PsiDirectory) targetDirectory else targetParent,
                targetFileName,
                extractedSuperName.quoteIfNeeded(),
                isExtractInterface,
                DocCommentPolicy<PsiComment>(docCommentPolicy)
        )
        refactoring(extractInfo)
    }
}