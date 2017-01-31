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
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.SyntheticPropertyAccessorReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.search.restrictToKotlinSources
import org.jetbrains.kotlin.idea.util.runReadActionInSmartMode
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.utils.ifEmpty

class KotlinOverridingMethodReferenceSearcher : MethodUsagesSearcher() {
    override fun processQuery(p: MethodReferencesSearch.SearchParameters, consumer: Processor<PsiReference>) {
        super.processQuery(p, consumer)

        val method = p.method
        p.project.runReadActionInSmartMode {
            val containingClass = method.containingClass ?: return@runReadActionInSmartMode

            val searchScope = p.effectiveSearchScope
                    .intersectWith(method.useScope)
                    .restrictToKotlinSources()
            if (searchScope === GlobalSearchScope.EMPTY_SCOPE) return@runReadActionInSmartMode

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

    override fun getTextOccurrenceProcessor(methods: Array<out PsiMethod>,
                                            aClass: PsiClass,
                                            strictSignatureSearch: Boolean): MethodTextOccurrenceProcessor {
        return object: MethodTextOccurrenceProcessor(aClass, strictSignatureSearch, *methods) {
            override fun processInexactReference(ref: PsiReference, refElement: PsiElement?, method: PsiMethod, consumer: Processor<PsiReference>): Boolean {
                val isGetter = JvmAbi.isGetterName(method.name)

                fun isWrongAccessorReference(): Boolean {
                    if (ref is KtSimpleNameReference) {
                        val readWriteAccess = ref.expression.readWriteAccess(true)
                        return readWriteAccess.isRead != isGetter && readWriteAccess.isWrite == isGetter
                    }
                    if (ref is SyntheticPropertyAccessorReference) {
                        return (ref is SyntheticPropertyAccessorReference.Getter) != isGetter
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

                var lightMethods = refElement.toLightMethods()
                        .filterNot { it.hasModifierProperty(PsiModifier.FINAL) }
                        .ifEmpty { return true }
                if (refElement is KtProperty || refElement is KtParameter) {
                    if (isWrongAccessorReference()) return true
                    lightMethods = lightMethods.filter { JvmAbi.isGetterName(it.name) == isGetter }
                }
                if (refElement is KtNamedFunction) {
                    lightMethods = lightMethods.filter { it.name == method.name }
                }

                return lightMethods.all { super.processInexactReference(ref, it, method, consumer) }
            }
        }
    }
}