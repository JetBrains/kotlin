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
import com.intellij.openapi.roots.impl.PackageDirectoryCache
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NonClasspathClassFinder
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.idea.caches.resolve.ScriptModuleSearchScope
import org.jetbrains.kotlin.load.java.AbstractJavaClassFinder
import org.jetbrains.kotlin.resolve.jvm.KotlinSafeClassFinder

class KotlinScriptDependenciesClassFinder(project: Project,
                                          private val scriptDependenciesManager: ScriptDependenciesManager
) : NonClasspathClassFinder(project), KotlinSafeClassFinder {

    private val myCaches by lazy {
        object : ConcurrentFactoryMap<VirtualFile, PackageDirectoryCache>() {
            override fun create(file: VirtualFile): PackageDirectoryCache? {
                val scriptClasspath = scriptDependenciesManager.getScriptClasspath(file)
                return createCache(scriptClasspath)
            }
        }
    }

    override fun calcClassRoots(): List<VirtualFile> = scriptDependenciesManager.getAllScriptsClasspath().toList()

    override fun getCache(scope: GlobalSearchScope?): PackageDirectoryCache =
            (scope as? ScriptModuleSearchScope ?:
             (scope as? AbstractJavaClassFinder.FilterOutKotlinSourceFilesScope)?.base as? ScriptModuleSearchScope
            )?.let {
                myCaches[it.scriptFile]
            } ?: super.getCache(scope)

    override fun clearCache() {
        super.clearCache()
        myCaches.clear()
    }

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        val psiClass = findClassInCache(qualifiedName, scope) ?: return null
        return when {
            scope is ScriptModuleSearchScope ||
            (scope as? AbstractJavaClassFinder.FilterOutKotlinSourceFilesScope)?.base is ScriptModuleSearchScope ||
            scope is EverythingGlobalScope ||
            psiClass.containingFile?.virtualFile.let { file ->
                file != null &&
                with(ProjectFileIndex.SERVICE.getInstance(myProject)) {
                    !isInContent(file) &&
                    !isInLibraryClasses(file) &&
                    !isInLibrarySource(file)
                }
            } -> psiClass
            else -> null
        }
    }

    private fun findClassInCache(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        if (qualifiedName.isEmpty()) return null

        return splitDotQualifiedName(qualifiedName).map { (packageName, classNames) ->
            findClassInPackage(packageName, classNames, scope)
        }.find { it != null }
    }

    private fun findClassInPackage(packageName: String, classNames: List<String>, scope: GlobalSearchScope): PsiClass? {
        var result: PsiClass? = null
        ContainerUtil.process(getCache(scope).getDirectoriesByPackageName(packageName)) { dir ->
            if (dir !in scope) return@process true

            findClassInDir(dir, classNames)?.let {
                result = it
                return@process false
            }
            return@process true
        }
        return result
    }

    private fun findClassInDir(dir: VirtualFile, classNames: List<String>): PsiClass? {
        val firstClassName = classNames.first()
        val virtualFile = dir.findChild("$firstClassName.class") ?: return null
        val psiFile = psiManager.findFile(virtualFile) as? PsiClassOwner ?: return null
        val topLevelClass = psiFile.classes.singleOrNull() ?: return null
        return classNames.subList(1, classNames.size).fold<String, PsiClass?>(topLevelClass) { currentPsiClass, className ->
            currentPsiClass?.findInnerClassByName(className, false)
        }
    }

    private fun splitDotQualifiedName(qualifiedName: String): Sequence<Pair<String, List<String>>> {
        val (packageName, className) = qualifiedName.splitByLastDot()

        return generateSequence(Pair(packageName, listOf(className))) {
            (prevPackageName, prevClassNames) ->
            if (prevPackageName == "") return@generateSequence null

            val (newPackageName, newTopLevelClassName) = prevPackageName.splitByLastDot()
            Pair(newPackageName, listOf(newTopLevelClassName) + prevClassNames)
        }
    }

    private fun String.splitByLastDot(): Pair<String, String> {
        return Pair(substringBeforeLast('.', missingDelimiterValue = ""), substringAfterLast('.'))
    }
}