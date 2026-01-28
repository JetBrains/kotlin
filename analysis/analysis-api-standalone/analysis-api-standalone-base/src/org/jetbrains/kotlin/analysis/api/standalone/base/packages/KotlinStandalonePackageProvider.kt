/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.packages

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.platform.packages.*
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Path
import kotlin.io.path.extension

class KotlinStandalonePackageProvider(
    project: Project,
    internal val scope: GlobalSearchScope,
    matchingPackageNames: Set<FqName>
) : KotlinPackageProviderBase(project, scope) {
    private val kotlinPackageToSubPackages: Map<FqName, Set<Name>> = run {
        val packages: MutableMap<FqName, MutableSet<Name>> = mutableMapOf() // the explicit type is here to workaround KTIJ-21172
        for (packageName in matchingPackageNames) {
            var currentPackage = FqName.ROOT
            for (subPackage in packageName.pathSegments()) {
                packages.getOrPut(currentPackage) { mutableSetOf() } += subPackage
                currentPackage = currentPackage.child(subPackage)
            }
            packages.computeIfAbsent(currentPackage) { mutableSetOf() }
        }
        packages
    }

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return packageFqName.isRoot || packageFqName in kotlinPackageToSubPackages
    }

    override fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name> {
        return kotlinPackageToSubPackages[packageFqName] ?: emptySet()
    }
}

class KotlinStandalonePackageProviderFactory(
    private val project: Project,
    private val indexedFiles: Collection<KtFile>,
    libraryRoots: List<VirtualFile>
) : KotlinCachingPackageProviderFactory(project) {
    /** A mapping between the library root (normally, a root entry in a JAR/KLib archive) and its containing KLib file. */
    private val klibFiles: Map<VirtualFile, Path> = buildMap {
        for (libraryRoot in libraryRoots) {
            val libraryFile = runCatching { VfsUtilCore.getVirtualFileForJar(libraryRoot)?.toNioPath() }.getOrNull() ?: continue
            if (libraryFile.extension.lowercase() == KLIB_FILE_EXTENSION) {
                put(libraryRoot, libraryFile)
            }
        }
    }

    /** On-demand package storage for [klibFiles]. */
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

    override fun createNewPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        val matchingPackageNames = buildSet {
            for (sourceKtFile in indexedFiles) {
                val sourceVirtualFile = sourceKtFile.virtualFile ?: continue
                if (searchScope.contains(sourceVirtualFile)) {
                    add(sourceKtFile.packageFqName)
                }
            }

            for ((libraryRoot, libraryFile) in klibFiles) {
                if (searchScope.contains(libraryRoot)) {
                    addAll(klibPackages[libraryFile] ?: emptyList())
                }
            }
        }

        return KotlinStandalonePackageProvider(project, searchScope, matchingPackageNames)
    }
}

class KotlinStandalonePackageProviderMerger(private val project: Project) : KotlinPackageProviderMerger {
    override fun merge(providers: List<KotlinPackageProvider>): KotlinPackageProvider =
        providers.mergeSpecificProviders<_, KotlinStandalonePackageProvider>(KotlinCompositePackageProvider.factory) { targetProviders ->
            val combinedScope = GlobalSearchScope.union(targetProviders.map { it.scope })
            project.createPackageProvider(combinedScope).apply {
                check(this is KotlinStandalonePackageProvider) {
                    "`${KotlinStandalonePackageProvider::class.simpleName}` can only be merged into a combined package provider of the same type."
                }
            }
        }
}
