/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.IdeUIModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants
import com.intellij.openapi.externalSystem.util.Order
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.libraries.Library
import org.jetbrains.kotlin.idea.configuration.klib.KotlinNativeLibraryNameUtil.KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE

// KT-30490. This `ProjectDataService` must be executed immediately after
// `com.intellij.openapi.externalSystem.service.project.manage.LibraryDataService` to clean-up KLIBs before any other actions taken on them.
@Order(ExternalSystemConstants.BUILTIN_LIBRARY_DATA_SERVICE_ORDER + 1) // force order
class KotlinNativeLibraryDataService : AbstractProjectDataService<LibraryData, Library>() {
    override fun getTargetDataKey() = ProjectKeys.LIBRARY

    // See also `com.intellij.openapi.externalSystem.service.project.manage.LibraryDataService.postProcess()`
    override fun postProcess(
        toImport: MutableCollection<DataNode<LibraryData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        if (projectData == null || modelsProvider is IdeUIModifiableModelsProvider) return

        val librariesModel = modelsProvider.modifiableProjectLibrariesModel
        val potentialOrphans = HashMap<String, Library>()

        librariesModel.libraries.forEach { library ->
            val libraryName = library.name?.takeIf { it.startsWith(KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) } ?: return@forEach
            potentialOrphans[libraryName] = library
        }

        if (potentialOrphans.isEmpty()) return

        modelsProvider.modules.forEach { module ->
            modelsProvider.getOrderEntries(module).forEach inner@{ orderEntry ->
                val libraryOrderEntry = orderEntry as? LibraryOrderEntry ?: return@inner
                if (libraryOrderEntry.isModuleLevel) return@inner

                val libraryName = (libraryOrderEntry.library?.name ?: libraryOrderEntry.libraryName)
                    ?.takeIf { it.startsWith(KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE) } ?: return@inner

                potentialOrphans.remove(libraryName)
            }
        }

        potentialOrphans.keys.forEach { libraryName ->
            librariesModel.getLibraryByName(libraryName)?.let { librariesModel.removeLibrary(it) }
        }
    }
}
