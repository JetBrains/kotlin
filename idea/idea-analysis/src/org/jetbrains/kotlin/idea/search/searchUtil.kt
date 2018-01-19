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

package org.jetbrains.kotlin.idea.search

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.types.expressions.OperatorConventions

infix fun SearchScope.and(otherScope: SearchScope): SearchScope = intersectWith(otherScope)
infix fun SearchScope.or(otherScope: SearchScope): SearchScope = union(otherScope)
infix fun GlobalSearchScope.or(otherScope: SearchScope): GlobalSearchScope = union(otherScope)
operator fun SearchScope.minus(otherScope: GlobalSearchScope): SearchScope = this and !otherScope
operator fun GlobalSearchScope.not(): GlobalSearchScope = GlobalSearchScope.notScope(this)

fun SearchScope.unionSafe(other: SearchScope): SearchScope {
    if (this is LocalSearchScope && this.scope.isEmpty()) {
        return other
    }
    if (other is LocalSearchScope && other.scope.isEmpty()) {
        return this
    }
    return this.union(other)
}

fun Project.allScope(): GlobalSearchScope = GlobalSearchScope.allScope(this)

fun Project.projectScope(): GlobalSearchScope = GlobalSearchScope.projectScope(this)

fun PsiFile.fileScope(): GlobalSearchScope = GlobalSearchScope.fileScope(this)

fun GlobalSearchScope.restrictByFileType(fileType: FileType) = GlobalSearchScope.getScopeRestrictedByFileTypes(this, fileType)

fun SearchScope.restrictByFileType(fileType: FileType) = when (this) {
    is GlobalSearchScope -> restrictByFileType(fileType)
    is LocalSearchScope -> {
        val elements = scope.filter { it.containingFile.fileType == fileType }
        when (elements.size) {
            0 -> GlobalSearchScope.EMPTY_SCOPE
            scope.size -> this
            else -> LocalSearchScope(elements.toTypedArray())
        }
    }
    else -> this
}

fun GlobalSearchScope.restrictToKotlinSources() = restrictByFileType(KotlinFileType.INSTANCE)

fun SearchScope.restrictToKotlinSources() = restrictByFileType(KotlinFileType.INSTANCE)

fun SearchScope.excludeKotlinSources(): SearchScope = excludeFileTypes(KotlinFileType.INSTANCE)

fun SearchScope.excludeFileTypes(vararg fileTypes: FileType): SearchScope {
    return if (this is GlobalSearchScope) {
        val includedFileTypes = FileTypeRegistry.getInstance().registeredFileTypes.filter { it !in fileTypes }.toTypedArray()
        GlobalSearchScope.getScopeRestrictedByFileTypes(this, *includedFileTypes)
    }
    else {
        this as LocalSearchScope
        val filteredElements = scope.filter { it.containingFile.fileType !in fileTypes }
        if (filteredElements.isNotEmpty())
            LocalSearchScope(filteredElements.toTypedArray())
        else
            GlobalSearchScope.EMPTY_SCOPE
    }
}

// Copied from SearchParameters.getEffectiveSearchScope()
fun ReferencesSearch.SearchParameters.effectiveSearchScope(element: PsiElement): SearchScope {
    if (element == elementToSearch) return effectiveSearchScope
    if (isIgnoreAccessScope) return scopeDeterminedByUser
    val accessScope = PsiSearchHelper.SERVICE.getInstance(element.project).getUseScope(element)
    return scopeDeterminedByUser.intersectWith(accessScope)
}

fun isOnlyKotlinSearch(searchScope: SearchScope): Boolean {
    return searchScope is LocalSearchScope && searchScope.scope.all { it.containingFile is KtFile }
}

fun PsiSearchHelper.isCheapEnoughToSearchConsideringOperators(
    name: String,
    scope: GlobalSearchScope,
    fileToIgnoreOccurrencesIn: PsiFile?,
    progress: ProgressIndicator?
): PsiSearchHelper.SearchCostResult {
    if (OperatorConventions.isConventionName(Name.identifier(name))) return PsiSearchHelper.SearchCostResult.TOO_MANY_OCCURRENCES

    return isCheapEnoughToSearch(name, scope, fileToIgnoreOccurrencesIn, progress)
}
