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

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.icons.AllIcons
import com.intellij.refactoring.classMembers.MemberInfoModel
import com.intellij.refactoring.ui.AbstractMemberSelectionTable
import com.intellij.ui.RowIcon
import org.jetbrains.kotlin.idea.JetIconProvider
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.psi.JetNamedFunction
import org.jetbrains.kotlin.psi.JetProperty
import javax.swing.Icon

public class KotlinMemberSelectionTable(
        memberInfos: List<KotlinMemberInfo>,
        memberInfoModel: MemberInfoModel<JetNamedDeclaration, KotlinMemberInfo>?,
        abstractColumnHeader: String?
) : AbstractMemberSelectionTable<JetNamedDeclaration, KotlinMemberInfo>(memberInfos, memberInfoModel, abstractColumnHeader) {
    override fun getAbstractColumnValue(memberInfo: KotlinMemberInfo): Any? {
        if (memberInfo.isStatic()) return null

        val member = memberInfo.member
        if (member !is JetNamedFunction && member !is JetProperty) return null

        if (member.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            myMemberInfoModel.isFixedAbstract(memberInfo)?.let { return it }
        }
        if (myMemberInfoModel.isAbstractEnabled(memberInfo)) return memberInfo.isToAbstract
        return myMemberInfoModel.isAbstractWhenDisabled(memberInfo)
    }

    override fun isAbstractColumnEditable(rowIndex: Int): Boolean {
        val memberInfo = myMemberInfos.get(rowIndex)

        if (memberInfo.isStatic()) return false

        val member = memberInfo.member
        if (member !is JetNamedFunction && member !is JetProperty) return false

        if (member.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
            myMemberInfoModel.isFixedAbstract(memberInfo)?.let { return false }
        }

        return memberInfo.isChecked && myMemberInfoModel.isAbstractEnabled(memberInfo)
    }

    override fun setVisibilityIcon(memberInfo: KotlinMemberInfo, icon: RowIcon) {
        icon.setIcon(JetIconProvider.getVisibilityIcon(memberInfo.member.modifierList), 1);
    }

    override fun getOverrideIcon(memberInfo: KotlinMemberInfo): Icon? {
        val defaultIcon = AbstractMemberSelectionTable.EMPTY_OVERRIDE_ICON

        val member = memberInfo.member
        if (member !is JetNamedFunction && member !is JetProperty) return defaultIcon

        return when (memberInfo.getOverrides()) {
            true -> AllIcons.General.OverridingMethod
            false -> AllIcons.General.ImplementingMethod
            else -> defaultIcon
        }
    }
}