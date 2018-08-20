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
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.RefactoringBundle
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.refactoring.checkConflictsInteractively
import org.jetbrains.kotlin.idea.refactoring.memberInfo.KotlinMemberInfo
import org.jetbrains.kotlin.idea.refactoring.memberInfo.getChildrenToAnalyze
import org.jetbrains.kotlin.idea.refactoring.memberInfo.resolveToDescriptorWrapperAware
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature

fun checkConflicts(project: Project,
                   sourceClass: KtClassOrObject,
                   targetClass: PsiNamedElement,
                   memberInfos: List<KotlinMemberInfo>,
                   onShowConflicts: () -> Unit = {},
                   onAccept: () -> Unit) {
    val conflicts = MultiMap<PsiElement, String>()

    val pullUpData = KotlinPullUpData(sourceClass,
                                      targetClass,
                                      memberInfos.mapNotNull { it.member })

    with(pullUpData) {
        for (memberInfo in memberInfos) {
            val member = memberInfo.member
            val memberDescriptor = member.resolveToDescriptorWrapperAware(resolutionFacade)

            checkClashWithSuperDeclaration(member, memberDescriptor, conflicts)
            checkAccidentalOverrides(member, memberDescriptor, conflicts)
            checkInnerClassToInterface(member, memberDescriptor, conflicts)
            checkVisibility(memberInfo, memberDescriptor, conflicts)
        }
    }
    checkVisibilityInAbstractedMembers(memberInfos, pullUpData.resolutionFacade, conflicts)

    project.checkConflictsInteractively(conflicts, onShowConflicts, onAccept)
}

internal fun checkVisibilityInAbstractedMembers(
    memberInfos: List<KotlinMemberInfo>,
    resolutionFacade: ResolutionFacade,
    conflicts: MultiMap<PsiElement, String>
) {
    val membersToMove = ArrayList<KtNamedDeclaration>()
    val membersToAbstract = ArrayList<KtNamedDeclaration>()

    for (memberInfo in memberInfos) {
        val member = memberInfo.member ?: continue
        (if (memberInfo.isToAbstract) membersToAbstract else membersToMove).add(member)
    }

    for (member in membersToAbstract) {
        val memberDescriptor = member.resolveToDescriptorWrapperAware(resolutionFacade)
        member.forEachDescendantOfType<KtSimpleNameExpression> {
            val target = it.mainReference.resolve() as? KtNamedDeclaration ?: return@forEachDescendantOfType
            if (!willBeMoved(target, membersToMove)) return@forEachDescendantOfType
            if (target.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                val targetDescriptor = target.resolveToDescriptorWrapperAware(resolutionFacade)
                val memberText = memberDescriptor.renderForConflicts()
                val targetText = targetDescriptor.renderForConflicts()
                val message = "$memberText uses $targetText which won't be accessible from the subclass."
                conflicts.putValue(target, message.capitalize())
            }
        }
    }
}

internal fun willBeMoved(element: PsiElement, membersToMove: Collection<KtNamedDeclaration>) =
    element.parentsWithSelf.any { it in membersToMove }

internal fun willBeUsedInSourceClass(
    member: PsiElement,
    sourceClass: KtClassOrObject,
    membersToMove: Collection<KtNamedDeclaration>
): Boolean {
    return !ReferencesSearch
        .search(member, LocalSearchScope(sourceClass), false)
        .all { willBeMoved(it.element, membersToMove) }
}

private val CALLABLE_RENDERER = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withOptions {
    parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
    modifiers = emptySet()
    startFromName = false
}

fun DeclarationDescriptor.renderForConflicts(): String {
    return when (this) {
        is ClassDescriptor -> "${DescriptorRenderer.getClassifierKindPrefix(this)} ${IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(this)}"
        is FunctionDescriptor -> "function '${CALLABLE_RENDERER.render(this)}'"
        is PropertyDescriptor -> "property '${CALLABLE_RENDERER.render(this)}'"
        else -> ""
    }
}

internal fun KotlinPullUpData.getClashingMemberInTargetClass(memberDescriptor: CallableMemberDescriptor): CallableMemberDescriptor? {
    val memberInSuper = memberDescriptor.substitute(sourceToTargetClassSubstitutor) ?: return null
    return targetClassDescriptor.findCallableMemberBySignature(memberInSuper as CallableMemberDescriptor)
}

private fun KotlinPullUpData.checkClashWithSuperDeclaration(
        member: KtNamedDeclaration,
        memberDescriptor: DeclarationDescriptor,
        conflicts: MultiMap<PsiElement, String>
) {
    val message = "${targetClassDescriptor.renderForConflicts()} already contains ${memberDescriptor.renderForConflicts()}"

    if (member is KtParameter) {
        if (((targetClass as? KtClass)?.primaryConstructorParameters ?: emptyList()).any { it.name == member.name }) {
            conflicts.putValue(member, message.capitalize())
        }
        return
    }

    if (memberDescriptor !is CallableMemberDescriptor) return

    val clashingSuper = getClashingMemberInTargetClass(memberDescriptor) ?: return
    if (clashingSuper.modality == Modality.ABSTRACT) return
    if (clashingSuper.kind != CallableMemberDescriptor.Kind.DECLARATION) return
    conflicts.putValue(member, message.capitalize())
}

private fun PsiClass.isSourceOrTarget(data: KotlinPullUpData): Boolean {
    var element = unwrapped
    if (element is KtObjectDeclaration && element.isCompanion()) element = element.containingClassOrObject

    return element == data.sourceClass || element == data.targetClass
}

private fun KotlinPullUpData.checkAccidentalOverrides(
        member: KtNamedDeclaration,
        memberDescriptor: DeclarationDescriptor,
        conflicts: MultiMap<PsiElement, String>) {
    if (memberDescriptor is CallableDescriptor && !member.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
        val memberDescriptorInTargetClass = memberDescriptor.substitute(sourceToTargetClassSubstitutor)
        if (memberDescriptorInTargetClass != null) {
            HierarchySearchRequest<PsiElement>(targetClass, targetClass.useScope)
                    .searchInheritors()
                    .asSequence()
                    .filterNot { it.isSourceOrTarget(this) }
                    .mapNotNull { it.unwrapped as? KtClassOrObject }
                    .forEach {
                        val subClassDescriptor = it.resolveToDescriptorWrapperAware(resolutionFacade) as ClassDescriptor
                        val substitutor = getTypeSubstitutor(targetClassDescriptor.defaultType,
                                                             subClassDescriptor.defaultType) ?: TypeSubstitutor.EMPTY
                        val memberDescriptorInSubClass =
                                memberDescriptorInTargetClass.substitute(substitutor) as? CallableMemberDescriptor
                        val clashingMemberDescriptor =
                                memberDescriptorInSubClass?.let { subClassDescriptor.findCallableMemberBySignature(it) } ?: return
                        val clashingMember = clashingMemberDescriptor.source.getPsi() ?: return

                        val message = memberDescriptor.renderForConflicts() +
                                      " in super class would clash with existing member of " +
                                      it.resolveToDescriptorWrapperAware(resolutionFacade).renderForConflicts()
                        conflicts.putValue(clashingMember, message.capitalize())
                    }
        }
    }
}

private fun KotlinPullUpData.checkInnerClassToInterface(
        member: KtNamedDeclaration,
        memberDescriptor: DeclarationDescriptor,
        conflicts: MultiMap<PsiElement, String>) {
    if (isInterfaceTarget && memberDescriptor is ClassDescriptor && memberDescriptor.isInner) {
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
        if (targetDescriptor in memberDescriptors.values) return
        val target = (targetDescriptor as? DeclarationDescriptorWithSource)?.source?.getPsi() ?: return
        if (targetDescriptor is DeclarationDescriptorWithVisibility
            && !Visibilities.isVisibleIgnoringReceiver(targetDescriptor, targetClassDescriptor)) {
            val message = RefactoringBundle.message(
                    "0.uses.1.which.is.not.accessible.from.the.superclass",
                    memberDescriptor.renderForConflicts(),
                    targetDescriptor.renderForConflicts()
            )
            conflicts.putValue(target, message.capitalize())
        }
    }

    val member = memberInfo.member
    val childrenToCheck = memberInfo.getChildrenToAnalyze()
    if (memberInfo.isToAbstract && member is KtCallableDeclaration) {
        if (member.typeReference == null) {
            (memberDescriptor as CallableDescriptor).returnType?.let { returnType ->
                val typeInTargetClass = sourceToTargetClassSubstitutor.substitute(returnType, Variance.INVARIANT)
                val descriptorToCheck = typeInTargetClass?.constructor?.declarationDescriptor as? ClassDescriptor
                if (descriptorToCheck != null) {
                    reportConflictIfAny(descriptorToCheck)
                }
            }
        }
    }

    childrenToCheck.forEach {
        it.accept(
                object : KtTreeVisitorVoid() {
                    override fun visitReferenceExpression(expression: KtReferenceExpression) {
                        super.visitReferenceExpression(expression)

                        val context = resolutionFacade.analyze(expression)
                        expression.references
                                .flatMap { (it as? KtReference)?.resolveToDescriptors(context) ?: emptyList() }
                                .forEach(::reportConflictIfAny)

                    }
                }
        )
    }
}