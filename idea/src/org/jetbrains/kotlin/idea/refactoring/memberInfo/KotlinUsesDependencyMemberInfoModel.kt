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

import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.classMembers.MemberInfoModel
import com.intellij.refactoring.util.classMembers.UsesDependencyMemberInfoModel
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

open class KotlinUsesDependencyMemberInfoModel<T : KtNamedDeclaration, M : MemberInfoBase<T>>(
        klass : KtClassOrObject,
        superClass: PsiNamedElement?,
        recursive: Boolean
) : UsesDependencyMemberInfoModel<T, PsiNamedElement, M>(klass, superClass, recursive) {
    override fun doCheck(memberInfo: M, problem: Int): Int {
        val member = memberInfo.member
        val container = member.containingClassOrObject
        if (problem == MemberInfoModel.ERROR
            && container is KtObjectDeclaration
            && container.isCompanion()
            && container.containingClassOrObject == myClass) return MemberInfoModel.WARNING

        return problem
    }
}