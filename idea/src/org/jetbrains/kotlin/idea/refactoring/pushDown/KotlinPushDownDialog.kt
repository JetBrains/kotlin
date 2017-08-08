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

package org.jetbrains.kotlin.idea.refactoring.pushDown

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.classMembers.*
import com.intellij.refactoring.ui.RefactoringDialog
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberSelectionPanel
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinUsesDependencyMemberInfoModel
import org.jetbrains.kotlin.idea.refactoring.memberInfo.qualifiedClassNameForRendering
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class KotlinPushDownDialog(
        project: Project,
        private val memberInfos: List<KotlinMemberInfo>,
        private val sourceClass: KtClass
) : RefactoringDialog(project, true) {
    init {
        title = PUSH_MEMBERS_DOWN
        init()
    }

    private var memberInfoModel: MemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>? = null

    private val selectedMemberInfos: List<KotlinMemberInfo>
        get() = memberInfos.filter { it.isChecked && memberInfoModel?.isMemberEnabled(it) ?: false }

    override fun getDimensionServiceKey() = "#" + this::class.java.name

    override fun createNorthPanel(): JComponent? {
        val gbConstraints = GridBagConstraints()

        val panel = JPanel(GridBagLayout())

        gbConstraints.insets = Insets(4, 0, 10, 8)
        gbConstraints.weighty = 1.0
        gbConstraints.weightx = 1.0
        gbConstraints.gridy = 0
        gbConstraints.gridwidth = GridBagConstraints.REMAINDER
        gbConstraints.fill = GridBagConstraints.BOTH
        gbConstraints.anchor = GridBagConstraints.WEST
        panel.add(JLabel(RefactoringBundle.message("push.members.from.0.down.label",
                                                   sourceClass.qualifiedClassNameForRendering())), gbConstraints)
        return panel
    }

    override fun createCenterPanel(): JComponent? {
        val panel = JPanel(BorderLayout())
        val memberSelectionPanel = KotlinMemberSelectionPanel(
                RefactoringBundle.message("members.to.be.pushed.down.panel.title"),
                memberInfos,
                RefactoringBundle.message("keep.abstract.column.header"))
        panel.add(memberSelectionPanel, BorderLayout.CENTER)

        memberInfoModel = object : DelegatingMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>(
                ANDCombinedMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>(
                        KotlinUsesDependencyMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>(sourceClass, null, false),
                        UsedByDependencyMemberInfoModel<KtNamedDeclaration, PsiNamedElement, KotlinMemberInfo>(sourceClass))
        ) {
            override fun isFixedAbstract(member: KotlinMemberInfo?) = null

            override fun isAbstractEnabled(memberInfo: KotlinMemberInfo): Boolean {
                val member = memberInfo.member
                if (member.hasModifier(KtTokens.INLINE_KEYWORD) ||
                    member.hasModifier(KtTokens.EXTERNAL_KEYWORD) ||
                    member.hasModifier(KtTokens.LATEINIT_KEYWORD)) return false
                return member is KtNamedFunction || member is KtProperty
            }
        }
        memberInfoModel!!.memberInfoChanged(MemberInfoChange(memberInfos))
        memberSelectionPanel.table.memberInfoModel = memberInfoModel
        memberSelectionPanel.table.addMemberInfoChangeListener(memberInfoModel)

        return panel
    }

    override fun doAction() {
        if (!isOKActionEnabled) return

        JavaRefactoringSettings.getInstance().PUSH_DOWN_PREVIEW_USAGES = isPreviewUsages

        invokeRefactoring(KotlinPushDownProcessor(project, sourceClass, selectedMemberInfos))
    }
}