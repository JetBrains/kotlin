/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.idea.asJava.LightClassProvider.Companion.providedCreateKtFakeLightMethod
import org.jetbrains.kotlin.idea.asJava.LightClassProvider.Companion.providedGetRepresentativeLightMethod
import org.jetbrains.kotlin.idea.asJava.LightClassProvider.Companion.providedIsKtFakeLightClass
import org.jetbrains.kotlin.idea.asJava.LightClassProvider.Companion.providedToLightMethods
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.findDeepestSuperMethodsNoWrapping
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.forEachKotlinOverride
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.forEachOverridingMethod
import org.jetbrains.kotlin.idea.search.KotlinSearchUsagesSupport.Companion.isOverridable
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.search.excludeKotlinSources
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import java.util.*

fun PsiElement.isOverridableElement(): Boolean = when (this) {
    is PsiMethod -> PsiUtil.canBeOverridden(this)
    is KtDeclaration -> isOverridable()
    else -> false
}

fun HierarchySearchRequest<*>.searchOverriders(): Query<PsiMethod> {
    val psiMethods = runReadAction { originalElement.providedToLightMethods() }
    if (psiMethods.isEmpty()) return EmptyQuery.getEmptyQuery()

    return psiMethods
        .map { psiMethod -> KotlinPsiMethodOverridersSearch.search(copy(psiMethod)) }
        .reduce { query1, query2 -> MergeQuery(query1, query2) }
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

        val psiClass = psiMethod.containingClass ?: return Collections.emptyList()

        val classToMethod = LinkedHashMap<PsiClass, PsiMethod>()
        val classTraverser = object : HierarchyTraverser<PsiClass> {
            override fun nextElements(current: PsiClass): Iterable<PsiClass> =
                DirectClassInheritorsSearch.search(
                    current,
                    current.project.allScope(),
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
                classToMethod[inheritor] = candidate
            }
        }

        return classToMethod.values
    }

    override fun isApplicable(request: HierarchySearchRequest<PsiMethod>): Boolean =
        runReadAction { request.originalElement.isOverridableElement() }

    override fun doSearchDirect(request: HierarchySearchRequest<PsiMethod>, consumer: Processor<in PsiMethod>) {
        searchDirectOverriders(request.originalElement).forEach { method -> consumer.process(method) }
    }
}

object PsiMethodOverridingHierarchyTraverser : HierarchyTraverser<PsiMethod> {
    override fun nextElements(current: PsiMethod): Iterable<PsiMethod> = KotlinPsiMethodOverridersSearch.searchDirectOverriders(current)
    override fun shouldDescend(element: PsiMethod): Boolean = PsiUtil.canBeOverridden(element)
}

fun PsiElement.toPossiblyFakeLightMethods(): List<PsiMethod> {
    if (this is PsiMethod) return listOf(this)

    val element = unwrapped ?: return emptyList()

    val lightMethods = element.providedToLightMethods()
    if (lightMethods.isNotEmpty()) return lightMethods

    return if (element is KtNamedDeclaration) listOfNotNull(providedCreateKtFakeLightMethod(element)) else emptyList()
}

fun KtNamedDeclaration.forEachOverridingElement(
    scope: SearchScope = runReadAction { useScope },
    processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean
): Boolean {
    val ktClass = runReadAction { containingClassOrObject as? KtClass } ?: return true

    providedToLightMethods().forEach { baseMethod ->
        if (!OverridingMethodsSearch.search(baseMethod, scope.excludeKotlinSources(), true).all { processor(baseMethod, it) }) return false
    }

    return forEachKotlinOverride(ktClass, listOf(this), scope) { baseElement, overrider -> processor(baseElement, overrider) }
}

fun PsiMethod.forEachImplementation(
    scope: SearchScope = runReadAction { useScope },
    processor: (PsiElement) -> Boolean
): Boolean {
    return forEachOverridingMethod(scope, processor)
            && FunctionalExpressionSearch.search(this, scope.excludeKotlinSources()).forEach(processor)
}

@Deprecated(
    "This method is obsolete and will be removed",
    ReplaceWith(
        "OverridersSearchUtilsKt.forEachOverridingMethod",
        "org.jetbrains.kotlin.idea.search.declarationsSearch.OverridersSearchUtilsKt"
    ),
    DeprecationLevel.ERROR
)
@JvmName("forEachOverridingMethod")
fun PsiMethod.forEachOverridingMethodCompat(
    scope: SearchScope = runReadAction { useScope },
    processor: (PsiMethod) -> Boolean
): Boolean = forEachOverridingMethod(scope, processor)

fun PsiClass.forEachDeclaredMemberOverride(processor: (superMember: PsiElement, overridingMember: PsiElement) -> Boolean) {
    val scope = runReadAction { useScope }

    if (!providedIsKtFakeLightClass()) {
        AllOverridingMethodsSearch.search(this, scope.excludeKotlinSources()).all { processor(it.first, it.second) }
    }

    val ktClass = unwrapped as? KtClass ?: return
    val members = ktClass.declarations.filterIsInstance<KtNamedDeclaration>() +
            ktClass.primaryConstructorParameters.filter { it.hasValOrVar() }
    forEachKotlinOverride(ktClass, members, scope, processor)
}

fun findDeepestSuperMethodsKotlinAware(method: PsiElement) =
    findDeepestSuperMethodsNoWrapping(method).mapNotNull { it.providedGetRepresentativeLightMethod() }