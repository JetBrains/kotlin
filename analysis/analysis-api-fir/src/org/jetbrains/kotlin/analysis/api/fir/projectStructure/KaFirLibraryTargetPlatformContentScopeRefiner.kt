/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.projectStructure

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KaModulePlatformKind
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinContentScopeRefiner
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinIntersectionScopeMergeTarget
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.toModulePlatformKind
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.library.components.KlibMetadataConstants.KLIB_METADATA_FILE_EXTENSION
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
 *
 * @see KaModulePlatformKind
 */
internal class KaFirLibraryTargetPlatformContentScopeRefiner : KotlinContentScopeRefiner {
    override fun getRestrictionScopes(module: KaModule): List<GlobalSearchScope> {
        if (module !is KaLibraryModule && module !is KaLibraryFallbackDependenciesModule) return emptyList()
        return listOf(createFilteringScope(module))
    }

    private fun createFilteringScope(module: KaModule): GlobalSearchScope =
        when (module.targetPlatform.toModulePlatformKind()) {
            KaModulePlatformKind.JVM -> KaFirJvmLibraryRestrictionScope(module.project)
            KaModulePlatformKind.JS -> KaFirKlibLibraryRestrictionScope(module.project)
            KaModulePlatformKind.WASM -> KaFirKlibLibraryRestrictionScope(module.project)
            KaModulePlatformKind.NATIVE -> KaFirKlibLibraryRestrictionScope(module.project)
            KaModulePlatformKind.METADATA -> KaFirCommonLibraryRestrictionScope(module.project)
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

/**
 * The JVM restriction scope excludes source files and certain binary file types.
 *
 * ### Binary files
 *
 * For binary files, the scope is formulated as a *denylist* rather than an allowlist of permitted extensions. An allowlist would have to
 * enumerate every binary file type that can legitimately carry JVM declarations, which is not practical: other JVM languages contribute
 * their own binary metadata file types that the Analysis API knows nothing about. For example, the Scala plugin loads its declarations from
 * `.tasty` files. It loads the PSI for `scala.Product` from `Product.tasty`, while the sibling `Product.class` is intentionally ignored. An
 * allowlist would filter `.tasty` out, leading to missing symbols in Kotlin/Scala interop (see KT-86402).
 *
 * On the other hand, the set of *Kotlin-specific* binary file types that should not be visible on the JVM platform is small. It is known to
 * the Analysis API and free of custom types, so a denylist captures it precisely while admitting `.tasty` and any other language's JVM
 * binary files for free.
 *
 * Note that `.kotlin_builtins` files are intentionally *not* excluded, as the JVM platform loads built-in declarations (such as `Any`) from
 * them.
 *
 * ### Source files
 *
 * For source files, the restriction scope has to exclude them explicitly. While a JVM library's binary content should not contain source
 * files, a malformed JAR may bundle sources next to classes (inside the *classes* root). Such source files must not be visible, as the
 * binary symbol providers would otherwise try to load declarations from sources instead of binaries.
 *
 * Rather than enumerating source extensions in a denylist (`.kt`, `.kts`, `.java`, and other JVM languages), the scope requires file types
 * to be binary. This ensures that source files from third-party JVM languages are excluded, which might otherwise be loaded via the Java
 * symbol provider.
 */
private class KaFirJvmLibraryRestrictionScope(project: Project) : GlobalSearchScope(project), KotlinIntersectionScopeMergeTarget {
    override fun contains(file: VirtualFile): Boolean {
        if (file.isDirectory) return true

        val extension = file.extension ?: return false

        // Fast path: Class files and Kotlin builtins.
        if (extension == JavaClassFileType.DEFAULT_EXTENSION || extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION) {
            return true
        }

        // `getFileTypeByExtension` classifies by extension only and never reads file content, unlike `VirtualFile.getFileType`.
        val fileType = FileTypeRegistry.getInstance().getFileTypeByExtension(extension)

        return fileType.isBinary && extension != METADATA_FILE_EXTENSION && extension != KLIB_METADATA_FILE_EXTENSION
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
