/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.packages

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProviderFactory
import org.jetbrains.kotlin.library.KlibConstants.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * A unified source of Kotlin package names shared between [KotlinStandalonePackageProvider] and
 * [org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProvider], so that both providers report
 * consistent package sets (KT-83760).
 *
 * Package names are computed with two strategies:
 *
 * - From the declaration index of [declarationProviderFactory], which covers source files and binary libraries when they are indexed as
 *   stubs (see [org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.STUBS]).
 * - From KLib metadata for KLib library roots.
 *
 * Kotlin classes in non-indexed JARs are currently not covered: in production Standalone mode, libraries are not indexed
 * (see [org.jetbrains.kotlin.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES]), and computing precise Kotlin-only
 * package names for a JAR requires distinguishing Kotlin class files from other JVM class files, which is expensive without an index.
 * Java packages in JARs are still found through `KotlinPackageProviderBase.doesPlatformSpecificPackageExist`. Kotlin packages in
 * non-indexed JARs should be supported in a follow-up to KT-83760.
 *
 * The provider must be registered as a project service whenever [KotlinStandaloneDeclarationProviderFactory] or
 * [KotlinStandalonePackageProviderFactory] is registered, with [declarationProviderFactory] being the same instance that is registered as
 * the `KotlinDeclarationProviderFactory`.
 */
class KotlinStandalonePackageNamesProvider(
    private val declarationProviderFactory: KotlinStandaloneDeclarationProviderFactory,
    libraryRoots: List<VirtualFile>,
) {
    companion object {
        fun getInstance(project: Project): KotlinStandalonePackageNamesProvider = project.service()
    }

    /**
     * A mapping from a KLib library root [VirtualFile] to the [Path] of the `.klib` file or unpacked KLib directory that contains it.
     * JAR roots are omitted (see the class documentation).
     */
    private val klibFiles: Map<VirtualFile, Path> = buildMap {
        for (libraryRoot in libraryRoots) {
            if (libraryRoot.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL) {
                // Root entry in the KLib archive
                val libraryFile = runCatching { VfsUtilCore.getVirtualFileForJar(libraryRoot)?.toNioPath() }.getOrNull() ?: continue
                if (libraryFile.extension.lowercase() == KLIB_FILE_EXTENSION) {
                    put(libraryRoot, libraryFile)
                }
            } else if (libraryRoot.isDirectory) {
                // Unpacked Kotlin library (a tree of directories with individual '.knm' files)
                val libraryFile = runCatching { libraryRoot.toNioPath() }.getOrNull() ?: continue
                put(libraryRoot, libraryFile)
            }
        }
    }

    private val klibPackages = Caffeine
        .newBuilder()
        .maximumSize(1000)
        .build<Path, List<FqName>> { libraryFile ->
            buildList {
                val kotlinLibraries = KlibLoader { libraryPaths(libraryFile) }.load().librariesStdlibFirst
                for (kotlinLibrary in kotlinLibraries) {
                    val moduleHeader = parseModuleHeader(kotlinLibrary.metadata.moduleHeaderData)
                    for (packageNameString in moduleHeader.packageFragmentNameList) {
                        add(FqName(packageNameString))
                    }
                }
            }
        }

    /**
     * Computes the package names of all indexed files and declarations contained in [scope]. This covers source files and, when binary
     * libraries are indexed as stubs, library declarations.
     *
     * The packages of indexed files are included even when a file contains no declarations, so that every package mentioned in a package
     * directive exists (consistent with the IDE, where packages are backed by a file-based index).
     */
    fun computePackageNamesFromIndex(scope: GlobalSearchScope): Set<FqName> = buildSet {
        addPackageNamesInScope(declarationProviderFactory.index.filesByPackage, scope)
        addPackageNamesInScope(declarationProviderFactory.index.classLikeDeclarationsByPackage, scope)
        addPackageNamesInScope(declarationProviderFactory.index.topLevelCallablesByPackage, scope)
    }

    private fun <T : KtElement> MutableSet<FqName>.addPackageNamesInScope(map: Map<FqName, Set<T>>, scope: GlobalSearchScope) {
        map.forEach { [fqName, elements] ->
            if (elements.any { it.containingKtFile.virtualFile in scope }) {
                add(fqName)
            }
        }
    }

    /**
     * Computes the package names of all KLib library roots contained in [scope], or `null` if no KLib root is contained in [scope].
     *
     * The `null` result allows callers to distinguish "no KLibs are tracked in this scope" from "the KLibs in this scope contain no
     * packages", e.g. to fall back to another computation for JAR-based library modules.
     */
    fun computeKlibPackageNames(scope: GlobalSearchScope): Set<FqName>? {
        var foundKlibRoot = false
        val packages = mutableSetOf<FqName>()

        for ([libraryRoot, libraryFile] in klibFiles) {
            if (scope.contains(libraryRoot)) {
                foundKlibRoot = true
                packages.addAll(klibPackages[libraryFile] ?: emptyList())
            }
        }

        return if (foundKlibRoot) packages else null
    }

    /**
     * Returns all Kotlin package names known to this provider in [scope]: the packages of indexed declarations and of KLib library
     * roots.
     */
    fun getPackageNamesInScope(scope: GlobalSearchScope): Set<FqName> =
        computePackageNamesFromIndex(scope) + (computeKlibPackageNames(scope) ?: emptySet())
}
