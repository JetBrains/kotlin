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
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.plugin.stubindex.JetSourceFilterScope

public class PluginDeclarationProviderFactoryService : DeclarationProviderFactoryService() {

    override fun create(
            project: Project,
            storageManager: StorageManager,
            syntheticFiles: Collection<JetFile>,
            filesScope: GlobalSearchScope
    ): DeclarationProviderFactory {
        return PluginDeclarationProviderFactory(project, JetSourceFilterScope.kotlinSources(filesScope, project),
                                                storageManager, syntheticFiles)
    }
}