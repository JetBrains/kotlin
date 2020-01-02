/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.memberInfo

import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.classMembers.ClassMembersRefactoringSupport
import com.intellij.refactoring.classMembers.DependentMembersCollectorBase
import com.intellij.refactoring.classMembers.MemberInfoBase
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.refactoring.pullUp.KotlinPullUpData
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.util.findCallableMemberBySignature

class KotlinClassMembersRefactoringSupport : ClassMembersRefactoringSupport {
    override fun isProperMember(memberInfo: MemberInfoBase<*>): Boolean {
        val member = memberInfo.member
        return member is KtNamedFunction || member is KtProperty || (member is KtParameter && member.isPropertyParameter()) || (member is KtClassOrObject && memberInfo.overrides == null)
    }

    override fun createDependentMembersCollector(clazz: Any, superClass: Any?): DependentMembersCollectorBase<*, *> {
        return object : DependentMembersCollectorBase<KtNamedDeclaration, PsiNamedElement>(
            clazz as KtClassOrObject,
            superClass as PsiNamedElement?
        ) {
            override fun collect(member: KtNamedDeclaration) {
                member.accept(
                    object : KtTreeVisitorVoid() {
                        private val pullUpData =
                            superClass?.let { KotlinPullUpData(clazz as KtClassOrObject, it as PsiNamedElement, emptyList()) }

                        private val possibleContainingClasses =
                            listOf(clazz) + ((clazz as? KtClass)?.companionObjects ?: emptyList())

                        override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                            val referencedMember = expression.mainReference.resolve() as? KtNamedDeclaration ?: return
                            val containingClassOrObject = referencedMember.containingClassOrObject ?: return
                            if (containingClassOrObject !in possibleContainingClasses) return

                            if (pullUpData != null) {
                                val memberDescriptor = referencedMember.unsafeResolveToDescriptor() as? CallableMemberDescriptor ?: return
                                val memberInSuper = memberDescriptor.substitute(pullUpData.sourceToTargetClassSubstitutor) ?: return
                                if (pullUpData.targetClassDescriptor
                                        .findCallableMemberBySignature(memberInSuper as CallableMemberDescriptor) != null
                                ) return
                            }

                            myCollection.add(referencedMember)
                        }
                    }
                )
            }
        }
    }
}