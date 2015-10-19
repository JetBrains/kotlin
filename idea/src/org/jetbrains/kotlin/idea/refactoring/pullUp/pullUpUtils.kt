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

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.anonymousObjectSuperTypeOrNull
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.isUnit

fun KtProperty.mustBeAbstractInInterface() =
        hasInitializer() || hasDelegate() || (!hasInitializer() && !hasDelegate() && accessors.isEmpty())

fun KtNamedDeclaration.canMoveMemberToJavaClass(targetClass: PsiClass): Boolean {
    return when (this) {
        is KtProperty -> {
            if (targetClass.isInterface) return false
            if (hasModifier(KtTokens.OPEN_KEYWORD) || hasModifier(KtTokens.ABSTRACT_KEYWORD)) return false
            if (accessors.isNotEmpty() || delegateExpression != null) return false
            true
        }
        is KtNamedFunction -> true
        else -> false
    }
}

fun addMemberToTarget(targetMember: KtNamedDeclaration, targetClass: KtClassOrObject): KtNamedDeclaration {
    val anchor = targetClass.declarations.filterIsInstance(targetMember.javaClass).lastOrNull()
    val movedMember = when {
        anchor == null && targetMember is KtProperty -> targetClass.addDeclarationBefore(targetMember, null)
        else -> targetClass.addDeclarationAfter(targetMember, anchor)
    }
    return movedMember as KtNamedDeclaration
}

fun doAddCallableMember(
        memberCopy: KtCallableDeclaration,
        clashingSuper: KtCallableDeclaration?,
        targetClass: KtClass): KtCallableDeclaration {
    if (clashingSuper != null && clashingSuper.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
        return clashingSuper.replaced(memberCopy)
    }
    return addMemberToTarget(memberCopy, targetClass) as KtCallableDeclaration
}

// TODO: Formatting rules don't apply here for some reason
fun KtNamedDeclaration.addModifierWithSpace(modifier: KtModifierKeywordToken) {
    addModifier(modifier)
    addAfter(KtPsiFactory(this).createWhiteSpace(), modifierList)
}

// TODO: Formatting rules don't apply here for some reason
fun KtNamedDeclaration.addAnnotationWithSpace(annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
    val result = addAnnotationEntry(annotationEntry)
    addAfter(KtPsiFactory(this).createWhiteSpace(), modifierList)
    return result
}

fun KtClass.makeAbstract() {
    if (!isInterface()) {
        addModifierWithSpace(KtTokens.ABSTRACT_KEYWORD)
    }
}

fun KtClassOrObject.getDelegatorToSuperClassByDescriptor(
        descriptor: ClassDescriptor,
        context: BindingContext
): KtDelegatorToSuperClass? {
    return getDelegationSpecifiers()
            .filterIsInstance<KtDelegatorToSuperClass>()
            .firstOrNull {
                val referencedType = context[BindingContext.TYPE, it.typeReference]
                referencedType?.constructor?.declarationDescriptor == descriptor
            }
}

fun makeAbstract(member: KtCallableDeclaration,
                 originalMemberDescriptor: CallableMemberDescriptor,
                 substitutor: TypeSubstitutor,
                 targetClass: KtClass) {
    if (!targetClass.isInterface()) {
        member.addModifierWithSpace(KtTokens.ABSTRACT_KEYWORD)
    }

    val builtIns = originalMemberDescriptor.builtIns
    if (member.typeReference == null) {
        var type = originalMemberDescriptor.returnType
        if (type == null || type.isError) {
            type = builtIns.nullableAnyType
        }
        else {
            type = substitutor.substitute(type.anonymousObjectSuperTypeOrNull() ?: type, Variance.INVARIANT)
                   ?: builtIns.nullableAnyType
        }

        if (member is KtProperty || !type.isUnit()) {
            member.setType(type, false)
        }
    }

    val deleteFrom = when (member) {
        is KtProperty -> {
            member.equalsToken ?: member.delegate ?: member.accessors.firstOrNull()
        }

        is KtNamedFunction -> {
            member.equalsToken ?: member.bodyExpression
        }

        else -> null
    }

    if (deleteFrom != null) {
        member.deleteChildRange(deleteFrom, member.lastChild)
    }
}

fun addDelegatorToSuperClass(
        delegator: KtDelegatorToSuperClass,
        targetClass: KtClassOrObject,
        targetClassDescriptor: ClassDescriptor,
        context: BindingContext,
        substitutor: TypeSubstitutor
) {
    val referencedType = context[BindingContext.TYPE, delegator.typeReference]!!
    val referencedClass = referencedType.constructor.declarationDescriptor as? ClassDescriptor ?: return

    if (targetClassDescriptor == referencedClass || DescriptorUtils.isDirectSubclass(targetClassDescriptor, referencedClass)) return

    val typeInTargetClass = substitutor.substitute(referencedType, Variance.INVARIANT)
    if (!(typeInTargetClass != null && !typeInTargetClass.isError)) return

    val renderedType = IdeDescriptorRenderers.SOURCE_CODE.renderType(typeInTargetClass)
    val newSpecifier = KtPsiFactory(targetClass).createDelegatorToSuperClass(renderedType)
    targetClass.addDelegationSpecifier(newSpecifier).addToShorteningWaitSet()
}