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

import com.intellij.psi.PsiMember
import com.intellij.refactoring.classMembers.MemberDependencyGraph
import com.intellij.refactoring.classMembers.MemberInfoBase
import com.intellij.refactoring.util.classMembers.InterfaceMemberDependencyGraph
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import java.util.*

class KotlinInterfaceMemberDependencyGraph<T : KtNamedDeclaration, M : MemberInfoBase<T>>(
        klass: KtClassOrObject
) : MemberDependencyGraph<T, M> {
    private val delegateGraph = InterfaceMemberDependencyGraph<PsiMember, MemberInfoBase<PsiMember>>(klass.toLightClass())

    override fun memberChanged(memberInfo: M) {
        delegateGraph.memberChanged(memberInfo.toJavaMemberInfo() ?: return)
    }

    @Suppress("UNCHECKED_CAST")
    override fun getDependent() = delegateGraph.dependent
        .asSequence()
        .mapNotNull { it.unwrapped }
            .filterIsInstanceTo(LinkedHashSet<KtNamedDeclaration>()) as Set<T>

    @Suppress("UNCHECKED_CAST")
    override fun getDependenciesOf(member: T): Set<T> {
        val psiMember = lightElementForMemberInfo(member) ?: return emptySet()
        val psiMemberDependencies = delegateGraph.getDependenciesOf(psiMember) ?: return emptySet()
        return psiMemberDependencies
            .asSequence()
            .mapNotNull { it.unwrapped }
                .filterIsInstanceTo(LinkedHashSet<KtNamedDeclaration>()) as Set<T>
    }
}
