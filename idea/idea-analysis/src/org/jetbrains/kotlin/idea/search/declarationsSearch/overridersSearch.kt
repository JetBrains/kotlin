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
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.EmptyQuery
import com.intellij.util.MergeQuery
import com.intellij.util.Processor
import com.intellij.util.Query
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.core.isOverridable
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtDeclaration
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
