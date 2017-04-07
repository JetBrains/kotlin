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
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.resolve.ScriptModuleSearchScope
import org.jetbrains.kotlin.load.java.AbstractJavaClassFinder
import org.jetbrains.kotlin.resolve.jvm.KotlinSafeClassFinder

class KotlinScriptDependenciesClassFinder(project: Project,
                                          private val kotlinScriptConfigurationManager: KotlinScriptConfigurationManager
) : NonClasspathClassFinder(project), KotlinSafeClassFinder {

    private val myCaches by lazy {
        object : ConcurrentFactoryMap<VirtualFile, PackageDirectoryCache>() {
            override fun create(file: VirtualFile): PackageDirectoryCache? {
                val scriptClasspath = kotlinScriptConfigurationManager.getScriptClasspath(file)
                val v = createCache(scriptClasspath)
                return v
            }
        }
    }

    override fun calcClassRoots(): List<VirtualFile> = kotlinScriptConfigurationManager.getAllScriptsClasspath().toList()

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

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? =
        super.findClass(qualifiedName, scope)?.let { aClass ->
            when {
                scope is ScriptModuleSearchScope ||
                (scope as? AbstractJavaClassFinder.FilterOutKotlinSourceFilesScope)?.base is ScriptModuleSearchScope ||
                scope is EverythingGlobalScope ||
                aClass.containingFile?.virtualFile.let { file ->
                    file != null &&
                    with (ProjectFileIndex.SERVICE.getInstance(myProject)) {
                        !isInContent(file) &&
                        !isInLibraryClasses(file) &&
                        !isInLibrarySource(file)
                    }
                } -> aClass
                else -> null
            }
        }
}