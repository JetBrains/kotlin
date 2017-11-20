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

package org.jetbrains.kotlin.idea.search.declarationsSearch

import com.intellij.psi.*
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.AllOverridingMethodsSearch
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.psi.search.searches.FunctionalExpressionSearch
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.EmptyQuery
import com.intellij.util.MergeQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtFakeLightClass
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.KtFakeLightMethod
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.excludeKotlinSources
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.substitutions.getTypeSubstitutor
import org.jetbrains.kotlin.util.findCallableMemberBySignature
import java.util.*

fun PsiElement.isOverridableElement(): Boolean = when (this) {
    is PsiMethod -> PsiUtil.canBeOverriden(this)
    is KtDeclaration -> isOverridable()
    else -> false
}

fun HierarchySearchRequest<*>.searchOverriders(): Query<PsiMethod> {
    val psiMethods = runReadAction { originalElement.toLightMethods() }
    if (psiMethods.isEmpty()) return EmptyQuery.getEmptyQuery()

    return psiMethods
            .map { psiMethod -> KotlinPsiMethodOverridersSearch.search(copy(psiMethod)) }
            .reduce { query1, query2 -> MergeQuery(query1, query2)}
}

object KotlinPsiMethodOverridersSearch : HierarchySearch<PsiMethod>(PsiMethodOverridingHierarchyTraverser) {
    fun searchDirectOverriders(psiMethod: PsiMethod): Iterable<PsiMethod> {
        fun PsiMethod.isAcceptable(inheritor: PsiClass, baseMethod: PsiMethod, baseClass: PsiClass): Boolean =
                when {
                    hasModifierProperty(PsiModifier.STATIC) -> false
                    baseMethod.hasModifierProperty(PsiModifier.PACKAGE_LOCAL) ->
                        JavaPsiFacade.getInstance(project).arePackagesTheSame(baseClass, inheritor)
                    else -> true
                }

        val psiClass = psiMethod.containingClass
        if (psiClass == null) return Collections.emptyList()

        val classToMethod = LinkedHashMap<PsiClass, PsiMethod>()
        val classTraverser = object : HierarchyTraverser<PsiClass> {
            override fun nextElements(current: PsiClass): Iterable<PsiClass> =
                    DirectClassInheritorsSearch.search(
                            current,
                            current.project.allScope(),
                            /* checkInheritance = */ true,
                            /* includeAnonymous = */ true
                    )

            override fun shouldDescend(element: PsiClass): Boolean =
                    element.isInheritable() && !classToMethod.containsKey(element)
        }

        classTraverser.forEach(psiClass) { inheritor ->
            val substitutor = TypeConversionUtil.getSuperClassSubstitutor(psiClass, inheritor, PsiSubstitutor.EMPTY)
            val signature = psiMethod.getSignature(substitutor)
            val candidate = MethodSignatureUtil.findMethodBySuperSignature(inheritor, signature, false)
            if (candidate != null && candidate.isAcceptable(inheritor, psiMethod, psiClass)) {
                classToMethod.put(inheritor, candidate)
            }
        }

        return classToMethod.values
    }

    override fun isApplicable(request: HierarchySearchRequest<PsiMethod>): Boolean =
            runReadAction { request.originalElement.isOverridableElement() }

    override fun doSearchDirect(request: HierarchySearchRequest<PsiMethod>, consumer: Processor<PsiMethod>) {
        searchDirectOverriders(request.originalElement).forEach { method -> consumer.process(method) }
    }
}

object PsiMethodOverridingHierarchyTraverser: HierarchyTraverser<PsiMethod> {
    override fun nextElements(current: PsiMethod): Iterable<PsiMethod> = KotlinPsiMethodOverridersSearch.searchDirectOverriders(current)
    override fun shouldDescend(element: PsiMethod): Boolean = PsiUtil.canBeOverriden(element)
}

fun PsiElement.toPossiblyFakeLightMethods(): List<PsiMethod> {
    if (this is PsiMethod) return listOf(this)

    val element = unwrapped ?: return emptyList()

    val lightMethods = element.toLightMethods()
    if (lightMethods.isNotEmpty()) return lightMethods

    return if (element is KtNamedDeclaration) listOfNotNull(KtFakeLightMethod.get(element)) else emptyList()
}

private fun forEachKotlinOverride(
        ktClass: KtClass,
        members: List<KtNamedDeclaration>,
        scope: SearchScope,
        processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
): Boolean {
    val baseClassDescriptor = runReadAction { ktClass.unsafeResolveToDescriptor() as ClassDescriptor }
    val baseDescriptors = runReadAction { members.mapNotNull { it.unsafeResolveToDescriptor() as? CallableMemberDescriptor }.filter { it.isOverridable } }
    if (baseDescriptors.isEmpty()) return true

    HierarchySearchRequest(ktClass, scope.restrictToKotlinSources(), true).searchInheritors().forEach {
        val inheritor = it.unwrapped as? KtClassOrObject ?: return@forEach
        val inheritorDescriptor = runReadAction { inheritor.unsafeResolveToDescriptor() as ClassDescriptor }
        val substitutor = getTypeSubstitutor(baseClassDescriptor.defaultType, inheritorDescriptor.defaultType) ?: return@forEach
        baseDescriptors.forEach {
            val superMember = it.source.getPsi()!!
            val overridingDescriptor = inheritorDescriptor.findCallableMemberBySignature(it.substitute(substitutor) as CallableMemberDescriptor)
            val overridingMember = overridingDescriptor?.source?.getPsi()
            if (overridingMember != null) {
                if (!processor(superMember, overridingMember)) return false
            }
        }
    }

    return true
}

fun KtNamedDeclaration.forEachOverridingElement(
        scope: SearchScope = runReadAction { useScope },
        processor: (PsiElement) -> Boolean
): Boolean {
    val ktClass = runReadAction { containingClassOrObject as? KtClass } ?: return true

    toLightMethods().forEach {
        if (!OverridingMethodsSearch.search(it, scope.excludeKotlinSources(), true).all(processor)) return false
    }

    return forEachKotlinOverride(ktClass, listOf(this), scope) { _, overrider -> processor(overrider) }
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
        val lightMethods = runReadAction { overrider.toPossiblyFakeLightMethods() }
        lightMethods.all { processor(it) }
    }
}

fun PsiMethod.forEachImplementation(
        scope: SearchScope = runReadAction { useScope },
        processor: (PsiElement) -> Boolean
): Boolean {
    return forEachOverridingMethod(scope, processor)
           && FunctionalExpressionSearch.search(this, scope.excludeKotlinSources()).forEach(processor)
}

fun PsiClass.forEachDeclaredMemberOverride(processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean) {
    val scope = runReadAction { useScope }

    if (this !is KtFakeLightClass) {
        AllOverridingMethodsSearch.search(this, scope.excludeKotlinSources()).all { processor(it.first, it.second) }
    }

    val ktClass = unwrapped as? KtClass ?: return
    val members = ktClass.declarations.filterIsInstance<KtNamedDeclaration>() +
                  ktClass.primaryConstructorParameters.filter { it.hasValOrVar() }
    forEachKotlinOverride(ktClass, members, scope, processor)
}

fun findDeepestSuperMethodsKotlinAware(method: PsiElement): List<PsiMethod> {
    val element = method.unwrapped
    return when (element) {
        is PsiMethod -> element.findDeepestSuperMethods().toList()
        is KtCallableDeclaration -> {
            val descriptor = element.resolveToDescriptorIfAny() as? CallableMemberDescriptor ?: return emptyList()
            descriptor.getDeepestSuperDeclarations(false).mapNotNull { it.source.getPsi()?.getRepresentativeLightMethod() }
        }
        else -> emptyList()
    }
}