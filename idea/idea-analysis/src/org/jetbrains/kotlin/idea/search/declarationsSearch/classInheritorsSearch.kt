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

import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.util.EmptyQuery
import com.intellij.util.Query
import org.jetbrains.kotlin.asJava.toLightClassWithBuiltinMapping
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtClassOrObject

fun HierarchySearchRequest<*>.searchInheritors(): Query<PsiClass> {
    val psiClass: PsiClass = when (originalElement) {
                                 is KtClassOrObject -> runReadAction { originalElement.toLightClassWithBuiltinMapping() }
                                 is PsiClass -> originalElement
                                 else -> null
                             } ?: return EmptyQuery.getEmptyQuery()

    return ClassInheritorsSearch.search(
            psiClass,
            searchScope,
            searchDeeply,
            /* checkInheritance = */ true,
            /* includeAnonymous = */ true
    )
}

fun PsiClass.isInheritable(): Boolean = !(this is PsiAnonymousClass || hasModifierProperty(PsiModifier.FINAL))
