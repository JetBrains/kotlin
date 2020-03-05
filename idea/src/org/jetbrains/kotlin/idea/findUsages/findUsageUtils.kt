/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.findUsages

import com.intellij.find.findUsages.FindUsagesManager
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.light.LightMemberReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.usageView.UsageInfo
import com.intellij.usages.UsageViewManager
import com.intellij.util.Query
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addIfNotNull

fun KtDeclaration.processAllExactUsages(
    options: FindUsagesOptions,
    processor: (UsageInfo) -> Unit
) {
    fun elementsToCheckReferenceAgainst(reference: PsiReference): List<PsiElement> {
        if (reference is KtReference) return listOf(this)
        return SmartList<PsiElement>().also { list ->
            list += this
            list += toLightElements()
            if (this is KtConstructor<*>) {
                list.addIfNotNull(getContainingClassOrObject().toLightClass())
            }
        }
    }

    val project = project
    FindUsagesManager(project, UsageViewManager.getInstance(project))
        .getFindUsagesHandler(this, true)
        ?.processElementUsages(
            this,
            { usageInfo ->
                val reference = usageInfo.reference ?: return@processElementUsages true
                if (reference is LightMemberReference || elementsToCheckReferenceAgainst(reference).any { reference.isReferenceTo(it) }) {
                    processor(usageInfo)
                }
                true
            },
            options
        )
}

fun KtDeclaration.processAllUsages(
    options: FindUsagesOptions,
    processor: (UsageInfo) -> Unit
) {
    val findUsagesHandler = KotlinFindUsagesHandlerFactory(project).createFindUsagesHandler(this, true)
    findUsagesHandler.processElementUsages(
        this,
        {
            processor(it)
            true
        },
        options
    )
}

object ReferencesSearchScopeHelper {
    fun search(declaration: KtDeclaration, defaultScope: SearchScope? = null): Query<PsiReference> {
        val enclosingElement = KtPsiUtil.getEnclosingElementForLocalDeclaration(declaration)
        return when {
            enclosingElement != null -> ReferencesSearch.search(declaration, LocalSearchScope(enclosingElement))
            defaultScope != null -> ReferencesSearch.search(declaration, defaultScope)
            else -> ReferencesSearch.search(declaration)
        }
    }
}