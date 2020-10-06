/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.psi.*
import com.intellij.psi.impl.search.MethodTextOccurrenceProcessor
import com.intellij.psi.impl.search.MethodUsagesSearcher
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.util.Processor
import org.jetbrains.kotlin.idea.asJava.LightClassProvider.Companion.providedToLightMethods
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.util.application.runReadActionInSmartMode
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty

class KotlinOverridingMethodReferenceSearcher : MethodUsagesSearcher() {
    override fun processQuery(p: MethodReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val method = p.method
        val isConstructor = p.project.runReadActionInSmartMode { method.isConstructor }
        if (isConstructor) {
            return
        }

        val searchScope = p.project.runReadActionInSmartMode {
            p.effectiveSearchScope
                .intersectWith(method.useScope)
                .restrictToKotlinSources()
        }

        if (searchScope === GlobalSearchScope.EMPTY_SCOPE) return

        super.processQuery(MethodReferencesSearch.SearchParameters(method, searchScope, p.isStrictSignatureSearch, p.optimizer), consumer)

        p.project.runReadActionInSmartMode {
            val containingClass = method.containingClass ?: return@runReadActionInSmartMode

            val nameCandidates = getPropertyNamesCandidatesByAccessorName(Name.identifier(method.name))
            for (name in nameCandidates) {
                p.optimizer.searchWord(
                    name.asString(),
                    searchScope,
                    UsageSearchContext.IN_CODE,
                    true,
                    method,
                    getTextOccurrenceProcessor(arrayOf(method), containingClass, false)
                )
            }
        }
    }

    override fun getTextOccurrenceProcessor(
        methods: Array<out PsiMethod>,
        aClass: PsiClass,
        strictSignatureSearch: Boolean
    ): MethodTextOccurrenceProcessor {
        return object : MethodTextOccurrenceProcessor(aClass, strictSignatureSearch, *methods) {
            override fun processInexactReference(
                ref: PsiReference,
                refElement: PsiElement?,
                method: PsiMethod,
                consumer: Processor<in PsiReference>
            ): Boolean {
                val isGetter = JvmAbi.isGetterName(method.name)

                fun isWrongAccessorReference(): Boolean {
                    if (ref is KtSimpleNameReference) {
                        val readWriteAccess = ref.expression.readWriteAccess(true)
                        return readWriteAccess.isRead != isGetter && readWriteAccess.isWrite == isGetter
                    }
                    if (ref is SyntheticPropertyAccessorReference) {
                        return ref.getter != isGetter
                    }
                    return false
                }

                if (refElement !is KtCallableDeclaration) {
                    if (isWrongAccessorReference()) return true
                    if (refElement !is PsiMethod) return true

                    val refMethodClass = refElement.containingClass ?: return true
                    val substitutor = TypeConversionUtil.getClassSubstitutor(myContainingClass, refMethodClass, PsiSubstitutor.EMPTY)
                    if (substitutor != null) {
                        val superSignature = method.getSignature(substitutor)
                        val refSignature = refElement.getSignature(PsiSubstitutor.EMPTY)

                        if (MethodSignatureUtil.isSubsignature(superSignature, refSignature)) {
                            return super.processInexactReference(ref, refElement, method, consumer)
                        }
                    }
                    return true
                }

                fun countNonFinalLightMethods() = refElement
                    .providedToLightMethods()
                    .filterNot { it.hasModifierProperty(PsiModifier.FINAL) }

                val lightMethods = when (refElement) {
                    is KtProperty, is KtParameter -> {
                        if (isWrongAccessorReference()) return true
                        countNonFinalLightMethods().filter { JvmAbi.isGetterName(it.name) == isGetter }
                    }

                    is KtNamedFunction ->
                        countNonFinalLightMethods().filter { it.name == method.name }

                    else ->
                        countNonFinalLightMethods()
                }


                return lightMethods.all { super.processInexactReference(ref, it, method, consumer) }
            }
        }
    }
}