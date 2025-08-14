/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.IncompatibleAbiVersion
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.InvalidLibraryFormat
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.LibraryNotFound
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblematicLibrary
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import org.jetbrains.kotlin.konan.file.File as KFile

/**
 * The [KlibLoader] component helps to load [KotlinLibrary]s based on the supplied
 * library paths (see [KlibLoaderSpec.libraryPaths]) and the specified settings,
 * for example, [KlibLoaderSpec.zipFileSystemAccessor].
 *
 * This component also performs a limited set of basic checks, such as
 * - Checking a platform and a specific target, see [KlibLoaderSpec.platformChecker]
 * - Checking ABI version compatibility, see [KlibLoaderSpec.maxPermittedAbiVersion]
 *
 * Any other checks, if they are necessary, should be performed outside [KlibLoader].
 *
 * Sorting of libraries, if it's really necessary, should also be performed outside
 * [KlibLoader].
 *
 * Note: [KlibLoader] does not add to the [KlibLoaderResult] any transitive
 * dependencies that any of the loaded libraries might have. Only the libraries
 * specified in [KlibLoaderSpec.libraryPaths] may appear in [KlibLoaderResult].
 */
class KlibLoader(init: KlibLoaderSpec.() -> Unit) {
    private val libraryPaths = ArrayList<String>()
    private var platformChecker: KlibPlatformChecker? = null
    private var maxPermittedAbiVersion: KotlinAbiVersion? = null
    private var zipFileSystemAccessor: ZipFileSystemAccessor? = null

    init {
        object : KlibLoaderSpec {
            override fun libraryPaths(paths: List<String>) {
                libraryPaths.addAll(paths)
            }

            override fun libraryPaths(vararg paths: String) {
                libraryPaths += paths
            }

            override fun platformChecker(checker: KlibPlatformChecker) {
                platformChecker = checker
            }

            override fun maxPermittedAbiVersion(abiVersion: KotlinAbiVersion) {
                maxPermittedAbiVersion = abiVersion
            }

            override fun zipFileSystemAccessor(accessor: ZipFileSystemAccessor) {
                zipFileSystemAccessor = accessor
            }
        }.init()
    }

    fun load(): KlibLoaderResult {
        // This is needed to avoid inspecting the same paths multiple times.
        // N.B. The original order of paths is preserved!
        val uniqueLibraryPaths = LinkedHashSet(libraryPaths)

        // And this is needed to avoid inspecting the same canonical paths more than once.
        // Note that transformation of a library path, which actually can be any form of
        // a relative path, to a canonical path happens after checking that the path is valid
        // and points to an existing file system object. That is why we have to keep both
        // [uniqueLibraryPaths] and [visitedCanonicalPaths] to have complete deduplication.
        val visitedCanonicalPaths = HashSet<Path>(uniqueLibraryPaths.size)

        val problematicLibraries = ArrayList<ProblematicLibrary>(uniqueLibraryPaths.size)
        val loadedLibrariesStdlibFirst = ArrayList<KotlinLibrary>(uniqueLibraryPaths.size)

        // Just to avoid accessing mutable nullable property, which does not work well with safe casts, in the loop below.
        val maxPermittedAbiVersion: KotlinAbiVersion? = maxPermittedAbiVersion

        uniqueLibraryPaths.forEach forEachLibraryPath@{ rawPath ->
            val validPath: Path? = if (rawPath.isEmpty())
                null
            else
                try {
                    Paths.get(rawPath)
                } catch (_: InvalidPathException) {
                    null
                }

            if (validPath == null || !validPath.exists()) {
                problematicLibraries += ProblematicLibrary(rawPath, LibraryNotFound)
                return@forEachLibraryPath
            }

            val canonicalPath: Path = validPath.toRealPath()
            if (!visitedCanonicalPaths.add(canonicalPath))
                return@forEachLibraryPath

            val library = createKotlinLibrary(
                KFile(validPath),
                component = KLIB_DEFAULT_COMPONENT_NAME,
                zipAccessor = zipFileSystemAccessor
            )

            val libraryVersions: KotlinLibraryVersioning = try {
                // Important: We wrap the first read operation with try-catch, as this is the simplest way
                // to check the correctness of the library layout. If the manifest, which is the essential
                // part of KLIB, is not available or corrupted, we immediately treat this library as problematic.
                // All later reads can be done outside the try-catch block.
                library.versions
            } catch (_: Exception) {
                problematicLibraries += ProblematicLibrary(rawPath, InvalidLibraryFormat)
                return@forEachLibraryPath
            }

            platformChecker?.check(library)?.let { platformCheckMismatch ->
                problematicLibraries += ProblematicLibrary(rawPath, platformCheckMismatch)
                return@forEachLibraryPath
            }

            if (maxPermittedAbiVersion != null) {
                val libraryAbiVersion: KotlinAbiVersion? = libraryVersions.abiVersion
                if (libraryAbiVersion == null || !libraryAbiVersion.isAtMost(maxPermittedAbiVersion)) {
                    problematicLibraries += ProblematicLibrary(
                        rawPath,
                        IncompatibleAbiVersion(
                            libraryVersions = libraryVersions,
                            minPermittedAbiVersion = null,
                            maxPermittedAbiVersion = maxPermittedAbiVersion
                        )
                    )
                    return@forEachLibraryPath
                }
            }

            // Stdlib is a special library. It many cases it should be the first to be deserialized
            // to make the necessary preparations (set up built-ins, etc.)
            // So, it's wise to explicitly set it to the first place even if sorting of libraries per se
            // is not the responsibility of [KlibLoader].
            if (library.isAnyPlatformStdlib)
                loadedLibrariesStdlibFirst.add(0, library)
            else
                loadedLibrariesStdlibFirst.add(library)
        }

        return KlibLoaderResult(loadedLibrariesStdlibFirst, problematicLibraries)
    }
}

interface KlibLoaderSpec {
    fun libraryPaths(paths: List<String>)
    fun libraryPaths(vararg paths: String)
    fun platformChecker(checker: KlibPlatformChecker)
    fun maxPermittedAbiVersion(abiVersion: KotlinAbiVersion)
    fun zipFileSystemAccessor(accessor: ZipFileSystemAccessor)
}
