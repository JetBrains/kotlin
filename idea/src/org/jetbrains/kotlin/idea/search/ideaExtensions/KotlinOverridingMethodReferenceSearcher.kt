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

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.search.MethodTextOccurrenceProcessor
import com.intellij.psi.impl.search.MethodUsagesSearcher
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.Processor
import org.jetbrains.kotlin.asJava.KotlinLightMethod
import org.jetbrains.kotlin.asJava.getRepresentativeLightMethod
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetFunction
import org.jetbrains.kotlin.psi.JetNamedDeclaration

public class KotlinOverridingMethodReferenceSearcher : MethodUsagesSearcher() {
    override fun getTextOccurrenceProcessor(methods: Array<out PsiMethod>,
                                            aClass: PsiClass,
                                            strictSignatureSearch: Boolean): MethodTextOccurrenceProcessor? {
        return object: MethodTextOccurrenceProcessor(aClass, strictSignatureSearch, *methods) {
            override fun processInexactReference(ref: PsiReference, refElement: PsiElement?, method: PsiMethod, consumer: Processor<PsiReference>): Boolean {
                if (refElement !is JetCallableDeclaration) return true
                return refElement.getRepresentativeLightMethod()?.let { super.processInexactReference(ref, it, method, consumer) } ?: true
            }
        }
    }
}