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
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.core.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.search.usagesSearch.DefaultSearchHelper
import org.jetbrains.kotlin.idea.search.usagesSearch.UsagesSearchTarget
import org.jetbrains.kotlin.idea.search.usagesSearch.search
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature

fun checkConflicts(project: Project,
                   sourceClass: JetClassOrObject,
                   targetClass: JetClass,
                   memberInfos: List<KotlinMemberInfo>,
                   onShowConflicts: () -> Unit = {},
                   onAccept: () -> Unit) {
    val conflicts = MultiMap<PsiElement, String>()

    val pullUpData = KotlinPullUpData(sourceClass, targetClass, memberInfos.map { it.getMember() })

    with(pullUpData) {
        for (memberInfo in memberInfos) {
            val member = memberInfo.getMember()
            val memberDescriptor = resolutionFacade.resolveToDescriptor(member)

            checkClashWithSuperDeclaration(member, memberDescriptor, conflicts)
            checkAccidentalOverrides(member, memberDescriptor, conflicts)
            checkInnerClassToInterface(member, memberDescriptor, conflicts)
            checkVisibility(memberInfo, memberDescriptor, conflicts)
        }
    }

    project.checkConflictsInteractively(conflicts, onShowConflicts, onAccept)
}

private val CALLABLE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.withOptions {
    parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    modifiers = emptySet()
    startFromName = false
}

private fun DeclarationDescriptor.renderForConflicts(): String {
    return when (this) {
        // todo: objects
        is ClassDescriptor -> "${DescriptorRenderer.getClassKindPrefix(this)} ${IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(this)}"
        is FunctionDescriptor -> "function '${CALLABLE_RENDERER.render(this)}'"
        is PropertyDescriptor -> "property '${CALLABLE_RENDERER.render(this)}'"
        else -> ""
    }
}

private fun KotlinPullUpData.getClashingMemberInTargetClass(memberDescriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
    val memberInSuper = memberDescriptor.substitute(sourceToTargetClassSubstitutor) ?: return null
    return targetClassDescriptor.findCallableMemberBySignature(memberInSuper as CallableMemberDescriptor)
}

private fun KotlinPullUpData.checkClashWithSuperDeclaration(
        member: JetNamedDeclaration,
        memberDescriptor: DeclarationDescriptor,
        conflicts: MultiMap<PsiElement, String>) {
    if (memberDescriptor is CallableMemberDescriptor) {
        val clashingSuper = getClashingMemberInTargetClass(memberDescriptor)
        if (clashingSuper != null && clashingSuper.getModality() != Modality.ABSTRACT) {
            val message = "${targetClassDescriptor.renderForConflicts()} already contains ${memberDescriptor.renderForConflicts()}"
            conflicts.putValue(member, message.capitalize())
        }
    }
}

private fun KotlinPullUpData.checkAccidentalOverrides(
        member: JetNamedDeclaration,
        memberDescriptor: DeclarationDescriptor,
        conflicts: MultiMap<PsiElement, String>) {
    if (memberDescriptor is CallableDescriptor && !member.hasModifier(JetTokens.PRIVATE_KEYWORD)) {
        val memberDescriptorInTargetClass = memberDescriptor.substitute(sourceToTargetClassSubstitutor)
        if (memberDescriptorInTargetClass != null) {
            HierarchySearchRequest<PsiElement>(targetClass, targetClass.getUseScope())
                    .searchInheritors()
                    .asSequence()
                    .filterNot { it.unwrapped == sourceClass || it.unwrapped == targetClass }
                    .map { it.unwrapped as? JetClassOrObject }
                    .filterNotNull()
                    .forEach {
                        val subClassDescriptor = resolutionFacade.resolveToDescriptor(it) as ClassDescriptor
                        val substitutor = getTypeSubstitutor(targetClassDescriptor.getDefaultType(),
                                                             subClassDescriptor.getDefaultType()) ?: TypeSubstitutor.EMPTY
                        val memberDescriptorInSubClass =
                                memberDescriptorInTargetClass.substitute(substitutor) as? CallableMemberDescriptor
                        val clashingMemberDescriptor =
                                memberDescriptorInSubClass?.let { subClassDescriptor.findCallableMemberBySignature(it) } ?: return
                        val clashingMember = clashingMemberDescriptor.getSource().getPsi() ?: return

                        val message = memberDescriptor.renderForConflicts() +
                                      " in super class would clash with existing member of " +
                                      resolutionFacade.resolveToDescriptor(it).renderForConflicts()
                        conflicts.putValue(clashingMember, message.capitalize())
                    }
        }
    }
}

private fun KotlinPullUpData.checkInnerClassToInterface(
        member: JetNamedDeclaration,
        memberDescriptor: DeclarationDescriptor,
        conflicts: MultiMap<PsiElement, String>) {
    if (targetClass.isInterface() && memberDescriptor is ClassDescriptor && memberDescriptor.isInner()) {
        val message = "${memberDescriptor.renderForConflicts()} is an inner class. It can not be moved to the interface"
        conflicts.putValue(member, message.capitalize())
    }
}

private fun KotlinPullUpData.checkVisibility(
        memberInfo: KotlinMemberInfo,
        memberDescriptor: DeclarationDescriptor,
        conflicts: MultiMap<PsiElement, String>
) {
    fun reportConflictIfAny(targetDescriptor: DeclarationDescriptor) {
        val target = (targetDescriptor as? DeclarationDescriptorWithSource)?.getSource()?.getPsi() ?: return
        if (targetDescriptor is DeclarationDescriptorWithVisibility
            && !Visibilities.isVisible(ReceiverValue.IRRELEVANT_RECEIVER, targetDescriptor, targetClassDescriptor)) {
            val message = RefactoringBundle.message(
                    "0.uses.1.which.is.not.accessible.from.the.superclass",
                    memberDescriptor.renderForConflicts(),
                    targetDescriptor.renderForConflicts()
            )
            conflicts.putValue(target, message.capitalize())
        }
    }

    val member = memberInfo.getMember()
    val childrenToCheck = member.allChildren.toArrayList()
    if (memberInfo.isToAbstract() && member is JetCallableDeclaration) {
        when (member) {
            is JetNamedFunction -> childrenToCheck.remove(member.getBodyExpression())
            is JetProperty -> {
                childrenToCheck.remove(member.getInitializer())
                childrenToCheck.remove(member.getDelegateExpression())
                childrenToCheck.removeAll(member.getAccessors())
            }
        }

        if (member.getTypeReference() == null) {
            (memberDescriptor as CallableDescriptor).getReturnType()?.let { returnType ->
                val typeInTargetClass = sourceToTargetClassSubstitutor.substitute(returnType, Variance.INVARIANT)
                val descriptorToCheck = typeInTargetClass?.getConstructor()?.getDeclarationDescriptor() as? ClassDescriptor
                if (descriptorToCheck != null) {
                    reportConflictIfAny(descriptorToCheck)
                }
            }
        }
    }

    childrenToCheck.forEach {
        it.accept(
                object : JetTreeVisitorVoid() {
                    override fun visitReferenceExpression(expression: JetReferenceExpression) {
                        super.visitReferenceExpression(expression)

                        val context = resolutionFacade.analyze(expression)
                        expression.getReferences()
                                .flatMap { (it as? JetReference)?.resolveToDescriptors(context) ?: emptyList() }
                                .forEach(::reportConflictIfAny)

                    }
                }
        )
    }
}
