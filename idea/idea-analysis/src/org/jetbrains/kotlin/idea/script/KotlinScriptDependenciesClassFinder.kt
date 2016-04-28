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

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.impl.PackageDirectoryCache
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.NonClasspathClassFinder
import com.intellij.psi.PsiClass
import com.intellij.psi.search.EverythingGlobalScope
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.kotlin.idea.caches.resolve.CustomizedScriptModuleSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl

@Suppress("unused") // project extension
class KotlinScriptDependenciesClassFinder(project: Project,
                                         private val kotlinScriptConfigurationManager: KotlinScriptConfigurationManager
) : NonClasspathClassFinder(project) {

    private val myCaches = object : ConcurrentFactoryMap<VirtualFile, PackageDirectoryCache>() {
        override fun create(file: VirtualFile): PackageDirectoryCache? {

            val scriptClasspath = kotlinScriptConfigurationManager.getScriptClasspath(file)
            val v = NonClasspathClassFinder.createCache(scriptClasspath)
            return v
        }
    }

    override fun calcClassRoots(): List<VirtualFile> = kotlinScriptConfigurationManager.getAllScriptsClasspath()

    override fun getCache(scope: GlobalSearchScope?): PackageDirectoryCache =
            (scope as? CustomizedScriptModuleSearchScope ?:
             (scope as? JavaClassFinderImpl.DelegatingGlobalSearchScopeWithBaseAccess)?.base as? CustomizedScriptModuleSearchScope
            )?.let {
                myCaches.get(it.scriptFile)
            } ?: super.getCache(scope)

    override fun clearCache() {
        super.clearCache()
        myCaches.clear()
    }

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? =
        super.findClass(qualifiedName, scope)?.let { aClass ->
            when {
                scope is CustomizedScriptModuleSearchScope ||
                (scope as? JavaClassFinderImpl.DelegatingGlobalSearchScopeWithBaseAccess)?.base is CustomizedScriptModuleSearchScope ||
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