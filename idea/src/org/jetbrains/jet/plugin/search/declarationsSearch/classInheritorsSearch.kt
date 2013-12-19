/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.search.declarationsSearch

import com.intellij.psi.search.SearchScope
import com.intellij.psi.PsiClass
import com.intellij.util.Query
import com.intellij.util.Processor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiAnonymousClass
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.plugin.stubindex.JetSuperClassIndex
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.plugin.search.usagesSearch.*
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.psi.psiUtil.contains
import com.intellij.psi.search.searches.DirectClassInheritorsSearch
import org.jetbrains.jet.lang.psi.psiUtil.isInheritable
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.asJava.KotlinLightClass
import com.intellij.psi.CommonClassNames
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import com.intellij.psi.search.searches.AllClassesSearch
import com.intellij.psi.PsiElement
import java.util.HashSet
import java.util.Stack
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.search.searches.ClassInheritorsSearch

fun PsiElement.isTopmostSuperClass(): Boolean = when (this) {
    is PsiClass -> getQualifiedName() == CommonClassNames.JAVA_LANG_OBJECT
    is JetClass -> descriptor == KotlinBuiltIns.getInstance().getAny()
    else -> false
}

fun PsiElement.isInheritableClass(): Boolean = when (this) {
    is PsiClass -> !(this is PsiAnonymousClass || hasModifierProperty(PsiModifier.FINAL))
    is JetClass -> isInheritable()
    else -> false
}

fun PsiElement.isTraitOrInterface(): Boolean = when (this) {
    is PsiClass -> isInterface()
    is JetClass -> isTrait()
    else -> false
}

public fun HierarchySearchRequest<PsiElement>.searchInheritors(): Query<PsiElement> = KotlinClassInheritorsSearch.search(this)

public object KotlinClassInheritorsSearch: DeclarationsSearch<PsiElement, HierarchySearchRequest<PsiElement>>() {
    protected override fun doSearch(request: HierarchySearchRequest<PsiElement>, consumer: Processor<PsiElement>) {
        if (request.searchDeeply) {
            doSearchAll(request, consumer)
        }
        else {
            doSearchDirect(request, consumer)
        }
    }

    protected override fun isApplicable(request: HierarchySearchRequest<PsiElement>): Boolean =
            request.originalElement.isInheritableClass()

    private fun doSearchDirect(request: HierarchySearchRequest<PsiElement>, consumer: Processor<PsiElement>) {
        fun searchByPsiClass(psiClass: PsiClass) {
            DirectClassInheritorsSearch.search(
                    aClass = psiClass,
                    scope = request.searchScope,
                    checkInheritance = false,
                    includeAnonymous = true
            ).iterator()
                    .map { inheritor -> inheritor.getNavigationElement() }
                    .all { currentWrapper -> consumer.process(currentWrapper) }
        }

        val originalElement = request.originalElement.getNavigationElement()

        when (originalElement) {
            is PsiClass ->
                searchByPsiClass(originalElement)

            is JetClassOrObject ->
                LightClassUtil.getPsiClass(originalElement)?.let { psiClass -> searchByPsiClass(psiClass) }
        }
    }

    private fun doSearchAll(request: HierarchySearchRequest<PsiElement>, consumer: Processor<PsiElement>) {
        if (request.originalElement.isTopmostSuperClass()) {
            AllClassesSearch.search(request.searchScope, request.project).all { psiClass -> consumer.process(psiClass) }
        }
        else {
            consumer.consumeHierarchy(request, ClassHierarchyTraverser)
        }
    }
}

object ClassHierarchyTraverser : HierarchyTraverser<PsiElement>() {
    override fun nextElements(current: PsiElement): Iterable<PsiElement> =
            HierarchySearchRequest<PsiElement>(
                    originalElement = current,
                    searchScope = GlobalSearchScope.allScope(current.getProject()),
                    searchDeeply = false
            ).searchInheritors()

    override fun shouldDescend(element: PsiElement): Boolean = element.isInheritableClass()
}