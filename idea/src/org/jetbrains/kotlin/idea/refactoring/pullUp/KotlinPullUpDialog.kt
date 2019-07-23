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
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import com.intellij.refactoring.classMembers.MemberInfoChange
import com.intellij.refactoring.classMembers.MemberInfoModel
import com.intellij.refactoring.memberPullUp.PullUpProcessor
import com.intellij.refactoring.util.DocCommentPolicy
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.refactoring.isCompanionMemberOf
import org.jetbrains.kotlin.idea.refactoring.isConstructorDeclaredProperty
import org.jetbrains.kotlin.idea.refactoring.isInterfaceClass
import org.jetbrains.kotlin.idea.refactoring.memberInfo.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class KotlinPullUpDialog(
        project: Project,
        classOrObject: KtClassOrObject,
        superClasses: List<PsiNamedElement>,
        memberInfoStorage: KotlinMemberInfoStorage
) : KotlinPullUpDialogBase(
        project, classOrObject, superClasses, memberInfoStorage, PULL_MEMBERS_UP
) {
    init {
        init()
    }

    private inner class MemberInfoModelImpl(
            originalClass: KtClassOrObject,
            superClass: PsiNamedElement?,
            interfaceContainmentVerifier: (KtNamedDeclaration) -> Boolean
    ) : KotlinUsesAndInterfacesDependencyMemberInfoModel<KtNamedDeclaration, KotlinMemberInfo>(
            originalClass,
            superClass,
            false,
            interfaceContainmentVerifier
    ) {
        private var lastSuperClass: PsiNamedElement? = null

        private fun KtNamedDeclaration.isConstructorParameterWithInterfaceTarget(targetClass: PsiNamedElement): Boolean {
            return targetClass is KtClass && targetClass.isInterface() && isConstructorDeclaredProperty()
        }

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
            if (superClass !is KtClass) return false

            val member = memberInfo.member
            if (member.hasModifier(KtTokens.INLINE_KEYWORD) ||
                member.hasModifier(KtTokens.EXTERNAL_KEYWORD) ||
                member.hasModifier(KtTokens.LATEINIT_KEYWORD)) return false
            if (member.isAbstractInInterface(sourceClass)) return false
            if (member.isConstructorParameterWithInterfaceTarget(superClass)) return false
            if (member.isCompanionMemberOf(sourceClass)) return false

            if (!superClass.isInterface()) return true

            return member is KtNamedFunction || (member is KtProperty && !member.mustBeAbstractInInterface()) || member is KtParameter
        }

        override fun isAbstractWhenDisabled(memberInfo: KotlinMemberInfo): Boolean {
            val superClass = superClass
            val member = memberInfo.member
            if (member.isCompanionMemberOf(sourceClass)) return false
            if (member.isAbstractInInterface(sourceClass)) return true
            if (superClass != null && member.isConstructorParameterWithInterfaceTarget(superClass)) return true
            return ((member is KtProperty || member is KtParameter) && superClass !is PsiClass)
                   || (member is KtNamedFunction && superClass is PsiClass)
        }

        override fun isMemberEnabled(memberInfo: KotlinMemberInfo): Boolean {
            val superClass = superClass ?: return false
            val member = memberInfo.member

            if (member.hasModifier(KtTokens.CONST_KEYWORD)) return false

            if (superClass is KtClass && superClass.isInterface() &&
                (member.hasModifier(KtTokens.INTERNAL_KEYWORD) || member.hasModifier(KtTokens.PROTECTED_KEYWORD))) return false

            if (superClass is PsiClass) {
                if (!member.canMoveMemberToJavaClass(superClass)) return false
                if (member.isCompanionMemberOf(sourceClass)) return false
            }
            if (memberInfo in memberInfoStorage.getDuplicatedMemberInfos(superClass)) return false
            if (member in memberInfoStorage.getExtending(superClass)) return false
            return true
        }

        override fun memberInfoChanged(event: MemberInfoChange<KtNamedDeclaration, KotlinMemberInfo>) {
            super.memberInfoChanged(event)
            val superClass = superClass ?: return
            if (superClass != lastSuperClass) {
                lastSuperClass = superClass
                val isInterface = superClass is KtClass && superClass.isInterface()
                event.changedMembers.forEach { it.isToAbstract = isInterface }
                setSuperClass(superClass)
            }
        }
    }

    private val memberInfoStorage: KotlinMemberInfoStorage get() = myMemberInfoStorage

    private val sourceClass: KtClassOrObject get() = myClass as KtClassOrObject

    override fun getDimensionServiceKey() = "#" + this::class.java.name

    override fun getSuperClass() = super.getSuperClass()

    override fun createMemberInfoModel(): MemberInfoModel<KtNamedDeclaration, KotlinMemberInfo> =
            MemberInfoModelImpl(sourceClass, preselection, getInterfaceContainmentVerifier { selectedMemberInfos })

    override fun getPreselection() = mySuperClasses.firstOrNull { !it.isInterfaceClass() } ?: mySuperClasses.firstOrNull()

    override fun createMemberSelectionTable(infos: MutableList<KotlinMemberInfo>) =
            KotlinMemberSelectionTable(infos, null, "Make abstract")

    override fun isOKActionEnabled() = selectedMemberInfos.size > 0

    override fun doAction() {
        val selectedMembers = selectedMemberInfos
        val targetClass = superClass!!
        checkConflicts(project, sourceClass, targetClass, selectedMembers, { close(DialogWrapper.OK_EXIT_CODE) }) {
            invokeRefactoring(createProcessor(sourceClass, targetClass, selectedMembers))
        }
    }

    companion object {
        fun createProcessor(sourceClass: KtClassOrObject,
                            targetClass: PsiNamedElement,
                            memberInfos: List<KotlinMemberInfo>): PullUpProcessor {
            val targetPsiClass = targetClass as? PsiClass ?: (targetClass as KtClass).toLightClass()
            return PullUpProcessor(
                sourceClass.toLightClass() ?: error("can't build lightClass for $sourceClass"),
                targetPsiClass,
                memberInfos.mapNotNull { it.toJavaMemberInfo() }.toTypedArray(),
                DocCommentPolicy<PsiComment>(KotlinRefactoringSettings.instance.PULL_UP_MEMBERS_JAVADOC))
        }
    }
}