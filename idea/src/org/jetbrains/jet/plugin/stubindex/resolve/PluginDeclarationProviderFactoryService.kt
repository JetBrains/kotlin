/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.stubindex.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.plugin.search.allScope

public class PluginDeclarationProviderFactoryService : DeclarationProviderFactoryService() {

    override fun create(project: Project, storageManager: StorageManager, files: Collection<JetFile>): DeclarationProviderFactory {
        val indexedSourcesScope = JetSourceFilterScope.kotlinSourcesAndLibraries(project.allScope())
        val nonIndexedFiles = files.filter {
            file ->
            !file.isPhysical() || !indexedSourcesScope.contains(file.getVirtualFile()!!)
        }
        val physicalFilesScope = GlobalSearchScope.filesScope(project, files.filter { it.isPhysical() }.map { it.getVirtualFile()!! })
        val indexedFilesScope = indexedSourcesScope.intersectWith(physicalFilesScope)

        return PluginDeclarationProviderFactory(project, indexedFilesScope, storageManager, nonIndexedFiles)
    }
}