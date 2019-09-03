/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NonClasspathClassFinder
import com.intellij.psi.PsiClass
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.resolve.jvm.KotlinSafeClassFinder


class KotlinScriptDependenciesClassFinder(
    private val project: Project
) : NonClasspathClassFinder(project), KotlinSafeClassFinder {
    override fun calcClassRoots(): List<VirtualFile> = ScriptDependenciesManager.getInstance(project)
        .getAllScriptsDependenciesClassFiles().toList()

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        tailrec fun findClassInner(parentQualifier: String, inners: List<String> = emptyList()): PsiClass? {
            if (parentQualifier.isEmpty()) return null
            val parentClass = super.findClass(parentQualifier, scope)
            if (parentClass != null) {
                if (inners.isNotEmpty()) {
                    val innerClass = inners.fold(parentClass) { c: PsiClass?, name: String ->
                        c?.findInnerClassByName(name, false)
                    }
                    if (innerClass != null) return innerClass
                } else return parentClass
            }
            return findClassInner(
                parentQualifier.substringBeforeLast('.', ""),
                listOf(parentQualifier.substringAfterLast('.')) + inners
            )
        }

        return findClassInner(qualifiedName)?.let { aClass ->
            if (scope is EverythingGlobalScope) return aClass

            val file = aClass.containingFile?.virtualFile ?: return null
            val index = ProjectFileIndex.SERVICE.getInstance(myProject)
            if (index.isInContent(file) || index.isInLibraryClasses(file) || index.isInLibrarySource(file)) {
                return null
            }
            return aClass
        }
    }
}