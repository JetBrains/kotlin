/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.native

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.library.KLIB_INTEROP_IR_PROVIDER_IDENTIFIER
import org.jetbrains.kotlin.konan.library.KlibNativeManifestTransformer
import org.jetbrains.kotlin.konan.library.SearchPathResolverWithTarget
import org.jetbrains.kotlin.konan.library.supportedTargetList
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryProperResolverWithAttributes
import org.jetbrains.kotlin.library.UnresolvedLibrary
import org.jetbrains.kotlin.library.loader.KlibLoader
import org.jetbrains.kotlin.library.loader.KlibPlatformChecker
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolver
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.util.Logger
import java.nio.file.Paths

/**
 * Creates a library resolver for native klib compilation using explicit paths.
 * This function avoids creating a Distribution instance.
 *
 * @param directLibs List of direct library paths provided by the user
 * @param target The target platform for compilation
 * @param stdlibPath Path to the stdlib klib (e.g., "<konanHome>/klib/common/stdlib")
 * @param platformLibrariesPath Path to the platform libraries directory (e.g., "<konanHome>/klib/platform/<target>")
 * @param logger Logger for warnings and errors
 * @param zipFileSystemAccessor Optional accessor for zip file systems
 */
fun createNativeKlibResolver(
    directLibs: List<String>,
    target: KonanTarget,
    stdlibPath: String?,
    platformLibrariesPath: String?,
    logger: Logger,
    zipFileSystemAccessor: ZipFileSystemAccessor? = null,
): KotlinLibraryResolver<KotlinLibrary> {
    // Compute distributionKlib from stdlibPath if available
    // stdlibPath is typically "<konanHome>/klib/common/stdlib"
    // distributionKlib should be "<konanHome>/klib"
    val distributionKlib = stdlibPath?.let {
        File(it).parentFile.parentFile.absolutePath
    }

    val searchPathResolver = NativeKlibExplicitPathResolver(
        directLibs = directLibs,
        target = target,
        distributionKlib = distributionKlib,
        platformLibrariesPath = platformLibrariesPath,
        logger = logger,
        zipFileSystemAccessor = zipFileSystemAccessor,
    )
    return searchPathResolver.libraryResolver(resolveManifestDependenciesLenient = true)
}

/**
 * A search path resolver for native klib that uses explicit paths instead of Distribution.
 * It extends [KotlinLibraryProperResolverWithAttributes] and overrides [distPlatformHead]
 * to use the explicit platform libraries path.
 */
private class NativeKlibExplicitPathResolver(
    directLibs: List<String>,
    override val target: KonanTarget,
    distributionKlib: String?,
    private val platformLibrariesPath: String?,
    override val logger: Logger,
    private val zipFileSystemAccessor: ZipFileSystemAccessor? = null,
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    directLibs = directLibs,
    distributionKlib = distributionKlib,
    skipCurrentDir = false,
    logger = logger,
    knownIrProviders = listOf(KLIB_INTEROP_IR_PROVIDER_IDENTIFIER)
), SearchPathResolverWithTarget<KotlinLibrary> {

    override fun libraryComponentBuilder(file: File, isDefault: Boolean): List<KotlinLibrary> =
        KlibLoader {
            libraryPaths(Paths.get(file.absolutePath).normalize())
            zipFileSystemAccessor?.let(::zipFileSystemAccessor)
            platformChecker(KlibPlatformChecker.NativeMetadata(target.name))
            manifestTransformer(KlibNativeManifestTransformer(target))
        }.load().librariesStdlibFirst

    override val distPlatformHead: File?
        get() = platformLibrariesPath?.let { File(it) }

    override fun libraryMatch(candidate: KotlinLibrary, unresolved: UnresolvedLibrary): Boolean {
        val resolverTarget = this.target
        val candidatePath = candidate.libraryFile.absolutePath

        val supportedTargets = candidate.supportedTargetList
        if (supportedTargets.isNotEmpty()) {
            if (resolverTarget.visibleName !in supportedTargets) {
                logger.strongWarning("KLIB resolver: Skipping '$candidatePath'. The target doesn't match. Expected '$resolverTarget', found $supportedTargets.")
                return false
            }
        }

        return super.libraryMatch(candidate, unresolved)
    }
}
