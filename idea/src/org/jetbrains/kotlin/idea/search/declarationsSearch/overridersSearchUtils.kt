/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search.declarationsSearch

import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.idea.caches.lightClasses.KtFakeLightMethod
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.search.excludeKotlinSources
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import java.util.*

fun forEachKotlinOverride(
    ktClass: KtClass,
    members: List<KtNamedDeclaration>,
    scope: SearchScope,
    processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
): Boolean {
    val baseClassDescriptor = runReadAction { ktClass.unsafeResolveToDescriptor() as ClassDescriptor }
    val baseDescriptors =
        runReadAction { members.mapNotNull { it.unsafeResolveToDescriptor() as? CallableMemberDescriptor }.filter { it.isOverridable } }
    if (baseDescriptors.isEmpty()) return true

    HierarchySearchRequest(ktClass, scope, true).searchInheritors().forEach(Processor { psiClass ->
        val inheritor = psiClass.unwrapped as? KtClassOrObject ?: return@Processor true
        runReadAction {
            val inheritorDescriptor = inheritor.unsafeResolveToDescriptor() as ClassDescriptor
            val substitutor =
                getTypeSubstitutor(baseClassDescriptor.defaultType, inheritorDescriptor.defaultType) ?: return@runReadAction true
            baseDescriptors.forEach {
                val superMember = it.source.getPsi()!!
                val overridingDescriptor = (it.substitute(substitutor) as? CallableMemberDescriptor)?.let { memberDescriptor ->
                    inheritorDescriptor.findCallableMemberBySignature(memberDescriptor)
                }
                val overridingMember = overridingDescriptor?.source?.getPsi()
                if (overridingMember != null) {
                    if (!processor(superMember, overridingMember)) return@runReadAction false
                }
            }
            true
        }
    })

    return true
}

fun PsiMethod.forEachOverridingMethod(
    scope: SearchScope = runReadAction { useScope },
    processor: (PsiMethod) -> Boolean
): Boolean {
    if (this !is KtFakeLightMethod) {
        if (!OverridingMethodsSearch.search(this, scope.excludeKotlinSources(), true).forEach(processor)) return false
    }

    val ktMember = this.unwrapped as? KtNamedDeclaration ?: return true
    val ktClass = runReadAction { ktMember.containingClassOrObject as? KtClass } ?: return true
    return forEachKotlinOverride(ktClass, listOf(ktMember), scope) { _, overrider ->
        val lightMethods = runReadAction { overrider.toPossiblyFakeLightMethods().distinctBy { it.unwrapped } }
        lightMethods.all { processor(it) }
    }
}

fun findDeepestSuperMethodsNoWrapping(method: PsiElement): List<PsiElement> {
    val element = method.unwrapped
    return when (element) {
        is PsiMethod -> element.findDeepestSuperMethods().toList()
        is KtCallableDeclaration -> {
            val descriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return emptyList()
            descriptor.getDeepestSuperDeclarations(false).mapNotNull {
                it.source.getPsi() ?: DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, it)
            }
        }
        else -> emptyList()
    }
}