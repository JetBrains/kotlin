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
import com.intellij.psi.impl.java.stubs.index.JavaFullClassNameIndex
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.resolve.jvm.KotlinSafeClassFinder


class KotlinScriptDependenciesClassFinder(
    private val project: Project
) : NonClasspathClassFinder(project), KotlinSafeClassFinder {
    override fun calcClassRoots(): List<VirtualFile> = ScriptConfigurationManager.getInstance(project)
        .getAllScriptsDependenciesClassFiles().filter { it.isValid }.toList()

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        val classByFileName = super.findClass(qualifiedName, scope)
        if (classByFileName != null) {
            return classByFileName.isInScope(scope)
        }

        // Following code is needed because NonClasspathClassFinder cannot find inner classes
        // JavaFullClassNameIndex cannot be used directly, because it filter only classes in source roots
        val classes = StubIndex.getElements(
            JavaFullClassNameIndex.getInstance().key,
            qualifiedName.hashCode(),
            project,
            scope,
            PsiClass::class.java
        ).filter {
            it.qualifiedName == qualifiedName
        }

        val found = when (classes.size) {
            0 -> null
            1 -> classes.single()
            else -> classes.first()  // todo: check when this happens
        }

        return found?.isInScope(scope)
    }

    private fun PsiClass.isInScope(scope: GlobalSearchScope): PsiClass? {
        if (scope is EverythingGlobalScope) return this

        val file = this.containingFile?.virtualFile ?: return null
        val index = ProjectFileIndex.SERVICE.getInstance(myProject)
        if (index.isInContent(file) || index.isInLibrary(file)) {
            return null
        }
        return this
    }
}