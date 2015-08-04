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

package org.jetbrains.kotlin.idea.refactoring.pullUp

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.JavaRefactoringSettings
import com.intellij.refactoring.classMembers.AbstractMemberInfoModel
import com.intellij.refactoring.memberPullUp.PullUpProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.refactoring.memberInfo.*
import org.jetbrains.kotlin.psi.*

public class KotlinPullUpDialog(
        project: Project,
        private val classOrObject: JetClassOrObject,
        superClasses: List<PsiNamedElement>,
        memberInfoStorage: KotlinMemberInfoStorage
) : KotlinPullUpDialogBase(
        project, classOrObject, superClasses, memberInfoStorage, PULL_MEMBERS_UP
) {
    init {
        init()
    }

    private inner class MemberInfoModelImpl : AbstractMemberInfoModel<JetNamedDeclaration, KotlinMemberInfo>() {
        // Abstract members remain abstract
        override fun isFixedAbstract(memberInfo: KotlinMemberInfo?) = true

        /*
         * Any non-abstract function can change abstractness.
         *
         * Non-abstract property with initializer or delegate is always made abstract.
         * Any other non-abstract property can change abstractness.
         *
         * Classes do not have abstractness
         */
        override fun isAbstractEnabled(memberInfo: KotlinMemberInfo): Boolean {
            val superClass = superClass ?: return false
            if (superClass is PsiClass) return false
            if (!superClass.isInterface()) return true

            val member = memberInfo.member
            return member is JetNamedFunction || (member is JetProperty && !member.mustBeAbstractInInterface())
        }

        override fun isAbstractWhenDisabled(memberInfo: KotlinMemberInfo): Boolean {
            val member = memberInfo.member
            return (member is JetProperty && superClass !is PsiClass) || (member is JetNamedFunction && superClass is PsiClass)
        }

        override fun isMemberEnabled(memberInfo: KotlinMemberInfo): Boolean {
            val superClass = superClass ?: return false
            val member = memberInfo.member

            if (superClass is PsiClass && !member.canMoveMemberToJavaClass(superClass)) return false
            if (memberInfo in memberInfoStorage.getDuplicatedMemberInfos(superClass)) return false
            if (member in memberInfoStorage.getExtending(superClass)) return false
            return true
        }
    }

    protected val memberInfoStorage: KotlinMemberInfoStorage get() = myMemberInfoStorage

    protected val sourceClass: JetClassOrObject get() = myClass as JetClassOrObject

    override fun getDimensionServiceKey() = "#" + javaClass.name

    override fun getSuperClass() = super.getSuperClass() as? JetClass

    override fun createMemberInfoModel() = MemberInfoModelImpl()

    override fun getPreselection() = mySuperClasses.firstOrNull()

    override fun createMemberSelectionTable(infos: MutableList<KotlinMemberInfo>) =
            KotlinMemberSelectionTable(infos, null, "Make abstract")

    override fun doAction() {
        val selectedMembers = selectedMemberInfos
        val targetClass = superClass!!
        checkConflicts(getProject(), sourceClass, targetClass, selectedMembers, { close(DialogWrapper.OK_EXIT_CODE) }) {
            invokeRefactoring(createProcessor(sourceClass, targetClass, selectedMembers))
        }
    }

    companion object {
        fun createProcessor(sourceClass: JetClassOrObject,
                            targetClass: PsiNamedElement,
                            memberInfos: List<KotlinMemberInfo>): PullUpProcessor {
            val targetPsiClass = targetClass as? PsiClass ?: (targetClass as JetClass).toLightClass()
            return PullUpProcessor(sourceClass.toLightClass(),
                                   targetPsiClass,
                                   memberInfos.map { it.toJavaMemberInfo() }.filterNotNull().toTypedArray(),
                                   DocCommentPolicy<PsiComment>(JavaRefactoringSettings.getInstance().PULL_UP_MEMBERS_JAVADOC))
        }
    }
}