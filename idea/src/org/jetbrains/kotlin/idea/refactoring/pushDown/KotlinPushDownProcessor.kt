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
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.pullUp.*
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import org.jetbrains.kotlin.utils.keysToMap
import java.util.ArrayList

public class KotlinPushDownProcessor(
        project: Project,
        private val sourceClass: JetClass,
        private val membersToMove: List<KotlinMemberInfo>
) : BaseRefactoringProcessor(project) {
    private val resolutionFacade = sourceClass.getResolutionFacade()

    private val sourceClassContext = resolutionFacade.analyzeFullyAndGetResult(listOf(sourceClass)).bindingContext

    private val sourceClassDescriptor = sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, sourceClass] as ClassDescriptor

    private val memberDescriptors = membersToMove
            .map { it.member }
            .keysToMap { sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]!! }

    inner class UsageViewDescriptorImpl : UsageViewDescriptor {
        override fun getProcessedElementsHeader() = RefactoringBundle.message("push.down.members.elements.header")

        override fun getElements() = arrayOf(sourceClass)

        override fun getCodeReferencesText(usagesCount: Int, filesCount: Int) =
                RefactoringBundle.message("classes.to.push.down.members.to", UsageViewBundle.getReferencesString(usagesCount, filesCount))

        override fun getCommentReferencesText(usagesCount: Int, filesCount: Int) = null
    }

    class SubclassUsage(element: PsiElement) : UsageInfo(element)

    override fun getCommandName() = PUSH_MEMBERS_DOWN

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>) = UsageViewDescriptorImpl()

    override fun getBeforeData() = RefactoringEventData().apply {
        addElement(sourceClass)
        addElements(membersToMove.map { it.member }.toTypedArray())
    }

    override fun getAfterData(usages: Array<out UsageInfo>) = RefactoringEventData().apply {
        addElements(usages.map { it.element as? JetClassOrObject }.filterNotNull())
    }

    override fun findUsages(): Array<out UsageInfo> {
        return HierarchySearchRequest(sourceClass, sourceClass.useScope, false)
                .searchInheritors()
                .map { it.unwrapped }
                .filterNotNull()
                .map { SubclassUsage(it) }
                .toTypedArray()
    }

    private fun pushDownToClass(targetClass: JetClassOrObject) {
        val targetClassDescriptor = resolutionFacade.resolveToDescriptor(targetClass) as ClassDescriptor
        val substitutor = getTypeSubstitutor(sourceClassDescriptor.defaultType, targetClassDescriptor.defaultType)
                          ?: TypeSubstitutor.EMPTY
        members@ for (memberInfo in membersToMove) {
            val member = memberInfo.member
            val memberDescriptor = memberDescriptors[member] ?: continue

            val movedMember = when (member) {
                is JetProperty, is JetNamedFunction -> {
                    memberDescriptor as CallableMemberDescriptor

                    val targetMemberDescriptor = memberDescriptor.substitute(substitutor)?.let {
                        targetClassDescriptor.findCallableMemberBySignature(it as CallableMemberDescriptor)
                    }
                    val targetMember = targetMemberDescriptor?.source?.getPsi() as? JetCallableDeclaration
                    targetMember?.apply {
                        if (memberDescriptor.modality != Modality.ABSTRACT && memberInfo.isToAbstract) {
                            addModifierWithSpace(JetTokens.OVERRIDE_KEYWORD)
                        }
                        else if (memberDescriptor.overriddenDescriptors.isEmpty()) {
                            removeModifier(JetTokens.OVERRIDE_KEYWORD)
                        }
                        else {
                            addModifierWithSpace(JetTokens.OVERRIDE_KEYWORD)
                        }
                    } ?: addMemberToTarget(member, targetClass).apply {
                        if (sourceClassDescriptor.kind == ClassKind.INTERFACE) {
                            if (targetClassDescriptor.kind != ClassKind.INTERFACE && memberDescriptor.modality == Modality.ABSTRACT) {
                                addModifierWithSpace(JetTokens.ABSTRACT_KEYWORD)
                            }
                        }
                        if (memberDescriptor.modality != Modality.ABSTRACT && memberInfo.isToAbstract) {
                            if (hasModifier(JetTokens.PRIVATE_KEYWORD)) {
                                addModifierWithSpace(JetTokens.PROTECTED_KEYWORD)
                            }
                            addModifierWithSpace(JetTokens.OVERRIDE_KEYWORD)
                        }
                    }
                }

                is JetClassOrObject -> {
                    if (memberInfo.overrides != null) {
                        sourceClass.getDelegatorToSuperClassByDescriptor(memberDescriptor as ClassDescriptor, sourceClassContext)?.let {
                            addDelegatorToSuperClass(it, targetClass, targetClassDescriptor, sourceClassContext, substitutor)
                        }
                        continue@members
                    }
                    else {
                        addMemberToTarget(member, targetClass)
                    }
                }

                else -> continue@members
            }
            applyMarking(movedMember, substitutor, targetClassDescriptor)
        }
    }

    private fun removeOriginalMembers() {
        for (memberInfo in membersToMove) {
            val member = memberInfo.member
            val memberDescriptor = memberDescriptors[member] ?: continue
            when (member) {
                is JetProperty, is JetNamedFunction -> {
                    member as JetCallableDeclaration
                    memberDescriptor as CallableMemberDescriptor

                    if (memberDescriptor.modality != Modality.ABSTRACT && memberInfo.isToAbstract) {
                        if (member.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
                            member.addModifierWithSpace(JetTokens.PROTECTED_KEYWORD)
                        }
                        makeAbstract(member, memberDescriptor, TypeSubstitutor.EMPTY, sourceClass)
                        member.typeReference?.addToShorteningWaitSet()
                    }
                    else {
                        member.delete()
                    }
                }
                is JetClassOrObject -> {
                    if (memberInfo.overrides != null) {
                        sourceClass.getDelegatorToSuperClassByDescriptor(memberDescriptor as ClassDescriptor, sourceClassContext)?.let {
                            sourceClass.removeDelegationSpecifier(it)
                        }
                    }
                    else {
                        member.delete()
                    }
                }
            }
        }
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val markedElements = ArrayList<JetElement>()
        try {
            membersToMove.forEach { markedElements += markElements(it.member, sourceClassContext, sourceClassDescriptor, null) }
            usages.forEach { (it.element as? JetClassOrObject)?.let { pushDownToClass(it) } }
            removeOriginalMembers()
        }
        finally {
            clearMarking(markedElements)
        }
    }
}