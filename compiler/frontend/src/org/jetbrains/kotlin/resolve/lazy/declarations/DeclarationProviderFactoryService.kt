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

package org.jetbrains.kotlin.resolve.lazy.declarations

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.storage.StorageManager
import com.intellij.openapi.vfs.VirtualFile
import java.util.HashSet
import kotlin.platform.platformStatic

public abstract class DeclarationProviderFactoryService {

    public abstract fun create(
            project: Project,
            storageManager: StorageManager,
            syntheticFiles: Collection<JetFile>,
            filesScope: GlobalSearchScope
    ): DeclarationProviderFactory

    default object {
        public platformStatic fun createDeclarationProviderFactory(
                project: Project,
                storageManager: StorageManager,
                syntheticFiles: Collection<JetFile>,
                filesScope: GlobalSearchScope
        ): DeclarationProviderFactory {
            return ServiceManager.getService(project, javaClass<DeclarationProviderFactoryService>())!!
                    .create(project, storageManager, syntheticFiles, filteringScope(syntheticFiles, filesScope))
        }

        private fun filteringScope(syntheticFiles: Collection<JetFile>, baseScope: GlobalSearchScope): GlobalSearchScope {
            if (syntheticFiles.isEmpty()) {
                return baseScope
            }
            return SyntheticFilesFilteringScope(syntheticFiles, baseScope)
        }
    }


    private class SyntheticFilesFilteringScope(syntheticFiles: Collection<JetFile>, baseScope: GlobalSearchScope)
        : DelegatingGlobalSearchScope(baseScope) {

        private val originals = syntheticFiles
                .map { it.getOriginalFile().getVirtualFile() }
                .filterNotNullTo(HashSet<VirtualFile>())

        override fun contains(file: VirtualFile) = super.contains(file) && file !in originals
    }
}
