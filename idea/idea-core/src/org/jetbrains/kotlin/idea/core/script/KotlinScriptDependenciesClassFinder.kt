/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
    project: Project,
    private val scriptDependenciesManager: ScriptDependenciesManager
) : NonClasspathClassFinder(project), KotlinSafeClassFinder {

    override fun calcClassRoots(): List<VirtualFile> = scriptDependenciesManager.getAllScriptsClasspath().toList()

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        tailrec fun findClassInner(parentQualifier: String, inners: List<String> = emptyList()): PsiClass? {
            if (parentQualifier.isEmpty()) return null
            val parentClass = super.findClass(parentQualifier, scope)
            if (parentClass != null) {
                if (inners.isNotEmpty()) {
                    val innerClass = inners.fold<String, PsiClass?>(parentClass) { c: PsiClass?, name: String ->
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