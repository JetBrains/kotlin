/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.packages

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.library.KlibConstants.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path
import kotlin.io.path.extension

/**
 * A unified source of package names shared between [KotlinStandalonePackageProvider] and
 * [org.jetbrains.kotlin.analysis.api.standalone.base.declarations.KotlinStandaloneDeclarationProvider].
 *
 * The provider computes packages from indexed [KtFile]s (sources and binary stubs) and KLib library roots. Sharing this computation
 * ensures both the package provider and the declaration provider report consistent package sets (KT-83760).
 *
 * [getPackageNamesInScope] returns `null` rather than an empty set when no tracked files or KLib roots intersect with the given scope.
 * This allows callers to distinguish "no packages in this scope" (empty set) from "this scope is not tracked by this provider" (null),
 * which is important for JAR-based library modules that are handled through separate platform-specific mechanisms.
 */
class KotlinStandalonePackageNamesProvider(
    indexedFilesProvider: () -> Collection<KtFile>,
    libraryRoots: List<VirtualFile>,
) {
    private val sourceFilesByPackage: Map<FqName, List<VirtualFile>> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        buildMap<FqName, MutableList<VirtualFile>> {
            for (ktFile in indexedFilesProvider()) {
                val virtualFile = ktFile.virtualFile ?: continue
                getOrPut(ktFile.packageFqName) { mutableListOf() }.add(virtualFile)
            }
        }
    }

    /**
     * A mapping from a KLib library root [VirtualFile] to the [Path] of the `.klib` file that contains it. Only KLib roots are included;
     * JAR roots are omitted because their packages are handled separately by
     * `KotlinStandaloneDeclarationProvider.computeBinaryLibraryModulePackageSet`.
     */
    private val klibFiles: Map<VirtualFile, Path> = buildMap {
        for (libraryRoot in libraryRoots) {
            if (libraryRoot.fileSystem.protocol == StandardFileSystems.JAR_PROTOCOL) {
                // Root entry in the Klib archive
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
     * Returns the set of package [FqName]s present in [scope], or `null` if no tracked files or KLib roots intersect with [scope].
     *
     * A `null` result means this provider has no information about [scope] (e.g., for JAR-based library modules). An empty set means the
     * provider knows about [scope] but finds no packages in it.
     */
    fun getPackageNamesInScope(scope: GlobalSearchScope): Set<FqName>? {
        val packages = mutableSetOf<FqName>()
        var foundTrackedEntity = false

        for ([fqName, virtualFiles] in sourceFilesByPackage) {
            if (virtualFiles.any { scope.contains(it) }) {
                foundTrackedEntity = true
                packages.add(fqName)
            }
        }

        for ([libraryRoot, libraryFile] in klibFiles) {
            if (scope.contains(libraryRoot)) {
                foundTrackedEntity = true
                packages.addAll(klibPackages[libraryFile] ?: emptyList())
            }
        }

        return if (foundTrackedEntity) packages else null
    }
}
