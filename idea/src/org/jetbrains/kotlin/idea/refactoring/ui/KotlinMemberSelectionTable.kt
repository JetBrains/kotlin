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

package org.jetbrains.kotlin.idea.refactoring.ui

import com.intellij.refactoring.classMembers.MemberInfoModel
import com.intellij.refactoring.ui.AbstractMemberSelectionTable
import com.intellij.ui.RowIcon
import com.intellij.util.IconUtil
import org.jetbrains.kotlin.idea.JetIconProvider
import org.jetbrains.kotlin.idea.refactoring.KotlinMemberInfo
import org.jetbrains.kotlin.psi.JetNamedDeclaration

public class KotlinMemberSelectionTable(
        memberInfos: List<KotlinMemberInfo>,
        memberInfoModel: MemberInfoModel<JetNamedDeclaration, KotlinMemberInfo>?,
        abstractColumnHeader: String?
) : AbstractMemberSelectionTable<JetNamedDeclaration, KotlinMemberInfo>(memberInfos, memberInfoModel, abstractColumnHeader) {
    override fun getAbstractColumnValue(memberInfo: KotlinMemberInfo) = null

    override fun isAbstractColumnEditable(rowIndex: Int) = false

    override fun setVisibilityIcon(memberInfo: KotlinMemberInfo, icon: RowIcon) {
        icon.setIcon(JetIconProvider.getVisibilityIcon(memberInfo.getMember().getModifierList()), 1);
    }

    override fun getOverrideIcon(memberInfo: KotlinMemberInfo) = AbstractMemberSelectionTable.EMPTY_OVERRIDE_ICON
}