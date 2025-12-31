/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib

import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryProperResolverWithAttributes
import org.jetbrains.kotlin.library.UnresolvedLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolver
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.util.Logger

object CommonKLibResolver {
    fun resolve(
        libraries: Collection<String>,
        logger: Logger,
        zipAccessor: ZipFileSystemAccessor? = null,
        lenient: Boolean = false,
        knownIrProviders: List<String> = listOf(),
        duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.DENY,
    ): KotlinLibraryResolveResult =
        resolveWithoutDependencies(
            libraries,
            logger,
            zipAccessor,
            lenient,
            knownIrProviders,
            duplicatedUniqueNameStrategy,
        ).resolveWithDependencies()

    fun resolveWithoutDependencies(
        libraries: Collection<String>,
        logger: Logger,
        zipAccessor: ZipFileSystemAccessor?,
        lenient: Boolean = false,
        knownIrProviders: List<String> = listOf(),
        duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy,
    ): KLibResolution {
        val unresolvedLibraries = libraries.map { UnresolvedLibrary(it, lenient) }
        val libraryAbsolutePaths = libraries.map { File(it).absolutePath }
        // Configure the resolver to only work with absolute paths for now.
        val libraryResolver = KLibResolverHelper(
            directLibs = libraryAbsolutePaths,
            distributionKlib = null,
            skipCurrentDir = false,
            logger = logger,
            zipAccessor = zipAccessor,
            knownIrProviders = knownIrProviders,
        ).libraryResolver(resolveManifestDependenciesLenient = true)

        return KLibResolution(
            libraryResolver,
            libraryResolver.resolveWithoutDependencies(
                unresolvedLibraries = unresolvedLibraries,
                noStdLib = true,
                noDefaultLibs = true,
                noEndorsedLibs = true,
                duplicatedUniqueNameStrategy = duplicatedUniqueNameStrategy,
            )
        )
    }
}

class KLibResolution(
    private val libraryResolver: KotlinLibraryResolver<KotlinLibrary>,
    val libraries: List<KotlinLibrary>,
) {
    fun resolveWithDependencies(): KotlinLibraryResolveResult {
        return with(libraryResolver) {
            libraries.resolveDependencies()
        }
    }
}

private class KLibResolverHelper(
    directLibs: List<String>,
    distributionKlib: String?,
    skipCurrentDir: Boolean,
    logger: Logger,
    private val zipAccessor: ZipFileSystemAccessor?,
    knownIrProviders: List<String>,
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    directLibs = directLibs,
    distributionKlib = distributionKlib,
    skipCurrentDir = skipCurrentDir,
    logger = logger,
    knownIrProviders = knownIrProviders,
) {
    // Stick with the default KotlinLibrary for now.
    override fun libraryComponentBuilder(file: File, isDefault: Boolean): List<KotlinLibrary> {
        return KlibLoader {
            libraryPaths(file.path)
            zipAccessor?.let(::zipFileSystemAccessor)
        }.load().librariesStdlibFirst
    }
}