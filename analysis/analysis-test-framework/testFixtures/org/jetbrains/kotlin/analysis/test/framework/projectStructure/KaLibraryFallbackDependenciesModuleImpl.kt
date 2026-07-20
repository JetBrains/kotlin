/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.projectStructure

import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.ProjectScope
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModuleBase
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.platform.TargetPlatform

class KaLibraryFallbackDependenciesModuleImpl(
    override val dependentLibrary: KaLibraryModule,
) : KaModuleBase(), KaLibraryFallbackDependenciesModule {
    override val directRegularDependencies: List<KaModule> get() = emptyList()
    override val directDependsOnDependencies: List<KaModule> get() = emptyList()
    override val directFriendDependencies: List<KaModule> get() = emptyList()

    @KaPlatformInterface
    override val baseContentScope: GlobalSearchScope
        get() {
            // Due to the contract of `KaLibraryFallbackDependenciesModule`, we have to make sure that the scope only contains *binary*
            // files. Unfortunately, we cannot easily build a "binaries-only" library scope from the Analysis API, as `ProjectScope` has no
            // endpoint for it, and we do not have access to platform classes like `ProjectAndLibrariesScope` used for the underlying
            // implementation. As a workaround in tests, we apply an explicit `BinaryOnlyScope`.
            return ProjectScope.getLibrariesScope(project)
                .intersectWith(BinaryOnlyScope(project))
                .intersectWith(GlobalSearchScope.notScope(dependentLibrary.contentScope))
        }

    override val targetPlatform: TargetPlatform
        get() = dependentLibrary.targetPlatform

    override val project: Project
        get() = dependentLibrary.project

    @KaExperimentalApi
    override val moduleDescription: String
        get() = "Fallback dependencies module for '${dependentLibrary.moduleDescription}'"
}

private class BinaryOnlyScope(project: Project) : GlobalSearchScope(project) {
    override fun contains(file: VirtualFile): Boolean {
        val extension = file.extension ?: return true
        return FileTypeRegistry.getInstance().getFileTypeByExtension(extension).isBinary
    }

    override fun isSearchInModuleContent(aModule: Module): Boolean = true
    override fun isSearchInLibraries(): Boolean = true
}
