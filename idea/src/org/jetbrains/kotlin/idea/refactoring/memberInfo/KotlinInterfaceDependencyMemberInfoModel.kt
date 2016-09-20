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

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.classMembers.DependencyMemberInfoModel
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.classMembers.MemberInfoModel
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.utils.ifEmpty

class KotlinInterfaceDependencyMemberInfoModel<T : KtNamedDeclaration, M : MemberInfoBase<T>>(
        aClass: KtClassOrObject
) : DependencyMemberInfoModel<T, M>(KotlinInterfaceMemberDependencyGraph<T, M>(aClass), MemberInfoModel.WARNING) {
    init {
        setTooltipProvider { memberInfo ->
            val dependencies = myMemberDependencyGraph.getDependenciesOf(memberInfo.member).ifEmpty { return@setTooltipProvider null }
            buildString {
                append(RefactoringBundle.message("interface.member.dependency.required.by.interfaces", dependencies.size))
                append(" ")
                dependencies.joinTo(this) { it.name ?: "" }
            }
        }
    }

    override fun isCheckedWhenDisabled(member: M) = false

    override fun isFixedAbstract(member: M) = null
}