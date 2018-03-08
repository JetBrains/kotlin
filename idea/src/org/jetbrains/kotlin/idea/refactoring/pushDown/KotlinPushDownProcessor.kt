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
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Ref
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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KtPsiClassWrapper
import org.jetbrains.kotlin.idea.refactoring.pullUp.*
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

class KotlinPushDownContext(
        val sourceClass: KtClass,
        val membersToMove: List<KotlinMemberInfo>
) {
    val resolutionFacade = sourceClass.getResolutionFacade()

    val sourceClassContext = resolutionFacade.analyzeFullyAndGetResult(listOf(sourceClass)).bindingContext

    val sourceClassDescriptor = sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, sourceClass] as ClassDescriptor

    val memberDescriptors = membersToMove
            .map { it.member }
            .keysToMap {
                when (it) {
                    is KtPsiClassWrapper -> it.psiClass.getJavaClassDescriptor(resolutionFacade)!!
                    else -> sourceClassContext[BindingContext.DECLARATION_TO_DESCRIPTOR, it]!!
                }
            }
}

class KotlinPushDownProcessor(
        project: Project,
        sourceClass: KtClass,
        membersToMove: List<KotlinMemberInfo>
) : BaseRefactoringProcessor(project) {
    private val context = KotlinPushDownContext(sourceClass, membersToMove)

    inner class UsageViewDescriptorImpl : UsageViewDescriptor {
        override fun getProcessedElementsHeader() = RefactoringBundle.message("push.down.members.elements.header")

        override fun getElements() = arrayOf(context.sourceClass)

        override fun getCodeReferencesText(usagesCount: Int, filesCount: Int) =
                RefactoringBundle.message("classes.to.push.down.members.to", UsageViewBundle.getReferencesString(usagesCount, filesCount))

        override fun getCommentReferencesText(usagesCount: Int, filesCount: Int) = null
    }

    class SubclassUsage(element: PsiElement) : UsageInfo(element)

    override fun getCommandName() = PUSH_MEMBERS_DOWN

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>) = UsageViewDescriptorImpl()

    override fun getBeforeData() = RefactoringEventData().apply {
        addElement(context.sourceClass)
        addElements(context.membersToMove.map { it.member }.toTypedArray())
    }

    override fun getAfterData(usages: Array<out UsageInfo>) = RefactoringEventData().apply {
        addElements(usages.mapNotNull { it.element as? KtClassOrObject })
    }

    override fun findUsages(): Array<out UsageInfo> {
        return HierarchySearchRequest(context.sourceClass, context.sourceClass.useScope, false)
                .searchInheritors()
                .mapNotNull { it.unwrapped }
                .map(::SubclassUsage)
                .toTypedArray()
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val usages = refUsages.get() ?: UsageInfo.EMPTY_ARRAY
        if (usages.isEmpty()) {
            val message = "${context.sourceClassDescriptor.renderForConflicts()} doesn't have inheritors\n" +
                          "Pushing members down will result in them being deleted. Would you like to proceed?"
            val answer = Messages.showYesNoDialog(message.capitalize(), PUSH_MEMBERS_DOWN, Messages.getWarningIcon())
            if (answer == Messages.NO) return false
        }

        val conflicts = myProject.runSynchronouslyWithProgress(RefactoringBundle.message("detecting.possible.conflicts"), true) {
            runReadAction { analyzePushDownConflicts(context, usages) }
        } ?: return false

        return showConflicts(conflicts, usages)
    }

    private fun pushDownToClass(targetClass: KtClassOrObject) {
        val targetClassDescriptor = context.resolutionFacade.resolveToDescriptor(targetClass) as ClassDescriptor
        val substitutor = getTypeSubstitutor(context.sourceClassDescriptor.defaultType, targetClassDescriptor.defaultType)
                          ?: TypeSubstitutor.EMPTY
        members@ for (memberInfo in context.membersToMove) {
            val member = memberInfo.member
            val memberDescriptor = context.memberDescriptors[member] ?: continue

            val movedMember = when (member) {
                is KtProperty, is KtNamedFunction -> {
                    memberDescriptor as CallableMemberDescriptor

                    moveCallableMemberToClass(
                            member as KtCallableDeclaration,
                            memberDescriptor,
                            targetClass,
                            targetClassDescriptor,
                            substitutor,
                            memberInfo.isToAbstract
                    )
                }

                is KtClassOrObject, is KtPsiClassWrapper -> {
                    if (memberInfo.overrides != null) {
                        context.sourceClass.getSuperTypeEntryByDescriptor(
                                memberDescriptor as ClassDescriptor,
                                context.sourceClassContext
                        )?.let {
                            addSuperTypeEntry(it, targetClass, targetClassDescriptor, context.sourceClassContext, substitutor)
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
        for (memberInfo in context.membersToMove) {
            val member = memberInfo.member
            val memberDescriptor = context.memberDescriptors[member] ?: continue
            when (member) {
                is KtProperty, is KtNamedFunction -> {
                    member as KtCallableDeclaration
                    memberDescriptor as CallableMemberDescriptor

                    if (memberDescriptor.modality != Modality.ABSTRACT && memberInfo.isToAbstract) {
                        if (member.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                            member.addModifier(KtTokens.PROTECTED_KEYWORD)
                        }
                        makeAbstract(member, memberDescriptor, TypeSubstitutor.EMPTY, context.sourceClass)
                        member.typeReference?.addToShorteningWaitSet()
                    }
                    else {
                        member.delete()
                    }
                }
                is KtClassOrObject, is KtPsiClassWrapper -> {
                    if (memberInfo.overrides != null) {
                        context.sourceClass.getSuperTypeEntryByDescriptor(
                                memberDescriptor as ClassDescriptor,
                                context.sourceClassContext
                        )?.let {
                            context.sourceClass.removeSuperTypeListEntry(it)
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
        val markedElements = ArrayList<KtElement>()
        try {
            context.membersToMove.forEach {
                markedElements += markElements(it.member, context.sourceClassContext, context.sourceClassDescriptor, null)
            }
            usages.forEach { (it.element as? KtClassOrObject)?.let { pushDownToClass(it) } }
            removeOriginalMembers()
        }
        finally {
            clearMarking(markedElements)
        }
    }
}