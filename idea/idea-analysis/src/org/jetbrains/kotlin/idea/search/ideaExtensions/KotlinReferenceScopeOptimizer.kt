/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.search.ideaExtensions

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.*
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.search.excludeFileTypes
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile

class KotlinReferenceScopeOptimizer : ScopeOptimizer {
    override fun getRestrictedUseScope(element: PsiElement): SearchScope? {
        if (element is KtCallableDeclaration && element.parent is KtFile) {
            return getRestrictedScopeForTopLevelCallable(element)
        }
        return null
    }

    private fun getRestrictedScopeForTopLevelCallable(callable: KtCallableDeclaration): GlobalSearchScope? {
        val useScope = callable.useScope as? GlobalSearchScope ?: return null
        val file = callable.parent as KtFile
        val packageName = file.packageFqName.takeUnless { it.isRoot } ?: return null
        val project = file.project
        val searchHelper = PsiSearchHelper.getInstance(project)

        val kotlinScope = GlobalSearchScope.getScopeRestrictedByFileTypes(useScope, KotlinFileType.INSTANCE)
        val javaScope = GlobalSearchScope.getScopeRestrictedByFileTypes(useScope, JavaFileType.INSTANCE)
        val restScope = useScope.excludeFileTypes(KotlinFileType.INSTANCE, JavaFileType.INSTANCE) as GlobalSearchScope

        val kotlinFiles = mutableListOf<VirtualFile>()
        searchHelper.processCandidateFilesForText(kotlinScope, UsageSearchContext.IN_CODE, true, packageName.asString()) {
            kotlinFiles.add(it)
        }

        val javaFiles = mutableListOf<VirtualFile>()
        searchHelper.processCandidateFilesForText(javaScope, UsageSearchContext.IN_CODE, true, file.javaFileFacadeFqName.asString()) {
            javaFiles.add(it)
        }

        return GlobalSearchScope.filesScope(project, kotlinFiles + javaFiles).uniteWith(restScope)
    }
}