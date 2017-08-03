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
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.EmptyQuery
import com.intellij.util.MergeQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.excludeKotlinSources
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
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

private fun forEachKotlinOverride(
        ktClass: KtClass,
        members: List<KtNamedDeclaration>,
        scope: SearchScope,
        processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
) {
    val baseClassDescriptor = runReadAction { ktClass.resolveToDescriptor() as ClassDescriptor }
    val baseDescriptors = runReadAction { members.mapNotNull { it.resolveToDescriptor() as? CallableMemberDescriptor }.filter { it.isOverridable } }
    if (baseDescriptors.isEmpty()) return

    HierarchySearchRequest(ktClass, scope.restrictToKotlinSources(), true).searchInheritors().forEach {
        val inheritor = (it as? KtLightClass)?.kotlinOrigin ?: return@forEach
        val inheritorDescriptor = runReadAction { inheritor.resolveToDescriptor() as ClassDescriptor }
        val substitutor = getTypeSubstitutor(baseClassDescriptor.defaultType, inheritorDescriptor.defaultType) ?: return@forEach
        baseDescriptors.forEach {
            val superMember = it.source.getPsi()!!
            val overridingDescriptor = inheritorDescriptor.findCallableMemberBySignature(it.substitute(substitutor) as CallableMemberDescriptor)
            val overridingMember = overridingDescriptor?.source?.getPsi()
            if (overridingMember != null) {
                if (!processor(superMember, overridingMember)) return
            }
        }
    }
}

fun PsiMethod.forEachOverridingMethod(processor: (PsiMethod) -> Boolean) {
    val scope = runReadAction { useScope }

    OverridingMethodsSearch.search(this, scope.excludeKotlinSources(), true).forEach(processor)

    val ktMember = (this as? KtLightMethod)?.kotlinOrigin as? KtNamedDeclaration ?: return
    val ktClass = runReadAction { ktMember.containingClassOrObject as? KtClass } ?: return
    forEachKotlinOverride(ktClass, listOf(ktMember), scope) { _, overrider ->
        val lightMethods = runReadAction { overrider.toLightMethods() }
        lightMethods.all { processor(it) }
    }
}

fun PsiClass.forEachDeclaredMemberOverride(processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean) {
    val scope = runReadAction { useScope }

    AllOverridingMethodsSearch.search(this, scope.excludeKotlinSources()).all { processor(it.first, it.second) }

    val ktClass = (this as? KtLightClass)?.kotlinOrigin as? KtClass ?: return
    val members = ktClass.declarations.filterIsInstance<KtNamedDeclaration>() +
                  ktClass.primaryConstructorParameters.filter { it.hasValOrVar() }
    forEachKotlinOverride(ktClass, members, scope, processor)
}