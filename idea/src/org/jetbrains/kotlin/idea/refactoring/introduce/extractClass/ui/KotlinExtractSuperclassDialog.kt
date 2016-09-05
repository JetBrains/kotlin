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
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel
import com.intellij.refactoring.classMembers.MemberInfoChange
import com.intellij.refactoring.extractSuperclass.ExtractSuperBaseDialog
import com.intellij.refactoring.extractSuperclass.JavaExtractSuperBaseDialog
import com.intellij.refactoring.util.DocCommentPolicy
import com.intellij.refactoring.util.RefactoringMessageUtil
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.quoteIfNeeded
import org.jetbrains.kotlin.idea.core.unquote
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.ExtractSuperclassInfo
import org.jetbrains.kotlin.idea.refactoring.introduce.extractClass.KotlinExtractSuperclassHandler
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel
import org.jetbrains.kotlin.idea.refactoring.memberInfo.extractClassMembers
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import java.awt.BorderLayout
import javax.swing.*

class KotlinExtractSuperclassDialog(
        originalClass: KtClassOrObject,
        private val targetParent: PsiElement,
        private val conflictChecker: (KotlinExtractSuperclassDialog) -> Boolean,
        private val refactoring: (ExtractSuperclassInfo) -> Unit
) : JavaExtractSuperBaseDialog(
        originalClass.project,
        originalClass.toLightClass()!!,
        emptyList(),
        KotlinExtractSuperclassHandler.REFACTORING_NAME
) {
    companion object {
        private val DESTINATION_PACKAGE_RECENT_KEY = "KotlinExtractSuperclassDialog.RECENT_KEYS"
    }

    val kotlinMemberInfos = extractClassMembers(originalClass)

    val selectedMembers: List<KotlinMemberInfo>
        get() = kotlinMemberInfos.filter { it.isChecked }

    private val fileNameField = JTextField()

    private val memberInfoModel = object : AbstractMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>() {
        override fun isFixedAbstract(memberInfo: KotlinMemberInfo?) = true

        override fun isAbstractEnabled(memberInfo: KotlinMemberInfo): Boolean {
            val member = memberInfo.member
            return member is KtNamedFunction || member is KtProperty
        }
    }.apply {
        memberInfoChanged(MemberInfoChange(kotlinMemberInfos))
    }

    val selectedTargetParent: PsiElement
        get() = if (targetParent is PsiDirectory) targetDirectory else targetParent

    val targetFileName: String
        get() = fileNameField.text

    init {
        init()

        fileNameField.text = "$extractedSuperName.${KotlinFileType.EXTENSION}"
    }

    override fun getDestinationPackageRecentKey() = DESTINATION_PACKAGE_RECENT_KEY

    override fun getClassNameLabelText() = RefactoringBundle.message("superclass.name")!!

    override fun getPackageNameLabelText() = RefactoringBundle.message("package.for.new.superclass")!!

    override fun getEntityName() = RefactoringBundle.message("ExtractSuperClass.superclass")!!

    override fun getTopLabelText() = RefactoringBundle.message("extract.superclass.from")!!

    override fun getDocCommentPolicySetting() = JavaRefactoringSettings.getInstance().EXTRACT_SUPERCLASS_JAVADOC

    override fun setDocCommentPolicySetting(policy: Int) {
        JavaRefactoringSettings.getInstance().EXTRACT_SUPERCLASS_JAVADOC = policy
    }

    override fun getDocCommentPanelName() = "KDoc for abstracts"

    override fun getExtractedSuperNameNotSpecifiedMessage() = RefactoringBundle.message("no.superclass.name.specified")!!

    override fun getHelpId() = HelpID.EXTRACT_SUPERCLASS

    override fun validateName(name: String): String? {
        return when {
            !KotlinNameSuggester.isIdentifier(name.quoteIfNeeded()) -> RefactoringMessageUtil.getIncorrectIdentifierMessage(name)
            name.unquote() == mySourceClass.name -> "Different name expected"
            else -> null
        }
    }

    override fun checkConflicts() = conflictChecker(this)

    override fun createActionComponent() = Box.createHorizontalBox()!!

    override fun createDestinationRootPanel(): JPanel? {
        if (targetParent !is PsiDirectory) return null

        val targetDirectoryPanel = super.createDestinationRootPanel()
        val targetFileNamePanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
            val label = JBLabel("Target file name:")
            add(label, BorderLayout.NORTH)
            label.labelFor = fileNameField
            add(fileNameField, BorderLayout.CENTER)
        }

        return FormBuilder
                .createFormBuilder()
                .addComponent(targetDirectoryPanel)
                .addComponent(targetFileNamePanel)
                .panel
    }

    override fun createNorthPanel(): JComponent? {
        return super.createNorthPanel().apply {
            if (targetParent !is PsiDirectory) {
                myPackageNameLabel.parent.remove(myPackageNameLabel)
                myPackageNameField.parent.remove(myPackageNameField)
            }
        }
    }

    override fun createCenterPanel(): JComponent? {
        return JPanel(BorderLayout()).apply {
            val memberSelectionPanel = KotlinMemberSelectionPanel(
                    RefactoringBundle.message("members.to.form.superclass"),
                    kotlinMemberInfos,
                    RefactoringBundle.message("make.abstract")
            )
            memberSelectionPanel.table.memberInfoModel = memberInfoModel
            memberSelectionPanel.table.addMemberInfoChangeListener(memberInfoModel)
            add(memberSelectionPanel, BorderLayout.CENTER)

            add(myDocCommentPanel, BorderLayout.EAST)
        }
    }

    override fun isExtractSuperclass() = true

    override fun preparePackage() {
        if (targetParent !is PsiDirectory) return

        super.preparePackage()

        val fileName = targetFileName
        if (!fileName.endsWith(".${KotlinFileType.EXTENSION}")) {
            throw ExtractSuperBaseDialog.OperationFailedException("Invalid Kotlin file name: $fileName")
        }
        RefactoringMessageUtil.checkCanCreateFile(myTargetDirectory, fileName)?.let {
            throw ExtractSuperBaseDialog.OperationFailedException(it)
        }
    }

    override fun createProcessor() = null

    override fun executeRefactoring() {
        val extractInfo = ExtractSuperclassInfo(
                mySourceClass.unwrapped as KtClassOrObject,
                selectedMembers,
                if (targetParent is PsiDirectory) targetDirectory else targetParent,
                targetFileName,
                extractedSuperName.quoteIfNeeded(),
                DocCommentPolicy<PsiComment>(docCommentPolicy)
        )
        refactoring(extractInfo)
    }
}