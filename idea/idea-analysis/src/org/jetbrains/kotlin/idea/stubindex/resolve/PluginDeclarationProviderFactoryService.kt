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

package org.jetbrains.kotlin.idea.stubindex.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleOrigin
import org.jetbrains.kotlin.idea.stubindex.KotlinSourceFilterScope
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.storage.StorageManager

class PluginDeclarationProviderFactoryService : DeclarationProviderFactoryService() {

    override fun create(
        project: Project,
        storageManager: StorageManager,
        syntheticFiles: Collection<KtFile>,
        filesScope: GlobalSearchScope,
        moduleInfo: ModuleInfo
    ): DeclarationProviderFactory {
        if (syntheticFiles.isEmpty() && (moduleInfo as IdeaModuleInfo).moduleOrigin != ModuleOrigin.MODULE) {
            // No actual source declarations for libraries
            // Even in case of libraries sources they should be obtained through the classpath with subsequent decompiling
            // Anyway, we'll filter them out with `KotlinSourceFilterScope.sources` call below
            return DeclarationProviderFactory.EMPTY
        }

        return PluginDeclarationProviderFactory(
            project,
            KotlinSourceFilterScope.projectSources(filesScope, project),
            storageManager,
            syntheticFiles,
            moduleInfo
        )
    }
}
