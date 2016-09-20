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
import com.intellij.refactoring.classMembers.ANDCombinedMemberInfoModel
import com.intellij.refactoring.classMembers.DelegatingMemberInfoModel
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.classMembers.MemberInfoModel
import com.intellij.refactoring.util.classMembers.UsesDependencyMemberInfoModel
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration

open class KotlinUsesAndInterfacesDependencyMemberInfoModel<T : KtNamedDeclaration, M : MemberInfoBase<T>>(
        klass: KtClassOrObject,
        superClass: PsiNamedElement?,
        recursive: Boolean,
        interfaceContainmentVerifier: (T) -> Boolean = { false }
) : DelegatingMemberInfoModel<T, M>(
        ANDCombinedMemberInfoModel(
                object : KotlinUsesDependencyMemberInfoModel<T, M>(klass, superClass, recursive) {
                    override fun checkForProblems(memberInfo: M): Int {
                        val problem = super.checkForProblems(memberInfo)
                        if (problem == MemberInfoModel.OK) return MemberInfoModel.OK

                        val member = memberInfo.member
                        if (interfaceContainmentVerifier(member)) return MemberInfoModel.OK

                        return problem
                    }
                },
                KotlinInterfaceDependencyMemberInfoModel<T, M>(klass))
) {
    @Suppress("UNCHECKED_CAST")
    fun setSuperClass(superClass: PsiNamedElement) {
        ((delegatingTarget as ANDCombinedMemberInfoModel<T, M>).model1 as UsesDependencyMemberInfoModel<T, PsiNamedElement, M>).setSuperClass(superClass)
    }
}
