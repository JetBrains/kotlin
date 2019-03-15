/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.facet

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import org.jetbrains.kotlin.idea.platform.tooling
import org.jetbrains.kotlin.idea.versions.bundledRuntimeVersion
import org.jetbrains.kotlin.platform.IdePlatformKind

class KotlinVersionInfoProviderByModuleDependencies : KotlinVersionInfoProvider {
    override fun getCompilerVersion(module: Module) = bundledRuntimeVersion()

    override fun getLibraryVersions(
        module: Module,
        platformKind: IdePlatformKind<*>,
        rootModel: ModuleRootModel?
    ): Collection<String> {
        val versionProvider = platformKind.tooling.getLibraryVersionProvider(module.project)
        return (rootModel ?: ModuleRootManager.getInstance(module))
                .orderEntries
                .asSequence()
                .filterIsInstance<LibraryOrderEntry>()
                .mapNotNull { libraryEntry -> libraryEntry.library?.let { versionProvider(it) } }
                .toList()
    }
}