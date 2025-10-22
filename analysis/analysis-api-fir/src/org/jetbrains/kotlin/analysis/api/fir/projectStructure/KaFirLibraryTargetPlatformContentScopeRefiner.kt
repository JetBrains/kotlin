/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.projectStructure

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinIntersectionScopeMergeTarget
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.platform.JsPlatform
import org.jetbrains.kotlin.platform.WasmPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.serialization.deserialization.METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol

/**
 * Restricts the content scopes of [KaLibraryModule]s and [KaLibraryFallbackDependenciesModule]s to files which are relevant to the module's
 * target platform.
 *
 * In general, a JAR or KLIB for a specific target platform should only contain the content that is relevant to that target platform. This
 * requirement is a general Analysis API requirement and should thus be applied by the Analysis API engine. Otherwise, all Analysis API
 * platforms would have to implement the same filtering.
 *
 * Analysis API platforms need to implement [KotlinProjectStructureProvider][org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinProjectStructureProvider]
 * consistently with the content scope restrictions.
 *
 * Because there is essentially a single restriction scope per target platform (and project), an intersection scope merger can easily factor
 * out the restriction scope to still allow the library base content scopes to be merged. As such, even with the restriction scope, library
 * content scopes are still mergeable.
 *
 * The content scope refiner is limited to the K2 implementation of the Analysis API: The K1 implementation doesn't honor the content scope
 * of a library module (while resolving calls, for example). If we restrict the content scope of a K1 `KaModule`, we can get unexpected
 * `KaBaseIllegalPsiException`s when we try to analyze a function symbol acquired via such call resolution.
 */
internal class KaFirLibraryTargetPlatformContentScopeRefiner : KotlinContentScopeRefiner {
    override fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> {
        if (module !is KaLibraryModule && module !is KaLibraryFallbackDependenciesModule) return emptyList()
        return listOf(createFilteringScope(module))
    }

    private fun createFilteringScope(module: KaModule): GlobalSearchScope {
        val targetPlatform = module.targetPlatform
        return when {
            targetPlatform.all { it is JvmPlatform } -> KaFirJvmLibraryRestrictionScope(module.project)
            targetPlatform.all { it is NativePlatform } -> KaFirKlibLibraryRestrictionScope(module.project)
            targetPlatform.all { it is JsPlatform } -> KaFirKlibLibraryRestrictionScope(module.project)
            targetPlatform.all { it is WasmPlatform } -> KaFirKlibLibraryRestrictionScope(module.project)
            else -> KaFirCommonLibraryRestrictionScope(module.project)
        }
    }
}

private class KaFirCommonLibraryRestrictionScope(project: Project) : GlobalSearchScope(project), KotlinIntersectionScopeMergeTarget {
    override fun contains(file: VirtualFile): Boolean {
        if (file.isDirectory) return true

        val extension = file.extension
        return extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION ||
                extension == METADATA_FILE_EXTENSION ||
                extension == KLIB_METADATA_FILE_EXTENSION
    }

    override fun isSearchInModuleContent(module: Module): Boolean = false
    override fun isSearchInLibraries(): Boolean = true

    override fun equals(other: Any?): Boolean =
        this === other || other is KaFirCommonLibraryRestrictionScope && project == other.project

    override fun hashCode(): Int = project.hashCode()
}

private class KaFirJvmLibraryRestrictionScope(project: Project) : GlobalSearchScope(project), KotlinIntersectionScopeMergeTarget {
    override fun contains(file: VirtualFile): Boolean {
        if (file.isDirectory) return true

        val extension = file.extension
        return extension == JavaClassFileType.INSTANCE.defaultExtension ||
                extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION
    }

    override fun isSearchInModuleContent(module: Module): Boolean = false
    override fun isSearchInLibraries(): Boolean = true

    override fun equals(other: Any?): Boolean =
        this === other || other is KaFirJvmLibraryRestrictionScope && project == other.project

    override fun hashCode(): Int = project.hashCode()
}

private class KaFirKlibLibraryRestrictionScope(project: Project) : GlobalSearchScope(project), KotlinIntersectionScopeMergeTarget {
    override fun contains(file: VirtualFile): Boolean = file.isDirectory || file.extension == KLIB_METADATA_FILE_EXTENSION

    override fun isSearchInModuleContent(module: Module): Boolean = false
    override fun isSearchInLibraries(): Boolean = true

    override fun equals(other: Any?): Boolean =
        this === other || other is KaFirKlibLibraryRestrictionScope && project == other.project

    override fun hashCode(): Int = project.hashCode()
}
