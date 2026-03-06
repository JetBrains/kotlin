/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.loader

import org.jetbrains.kotlin.konan.file.ZipFileSystemAccessor
import org.jetbrains.kotlin.konan.file.ZipFileSystemInPlaceAccessor
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.KlibImpl
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.IncompatibleAbiVersion
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.InvalidLibraryFormat
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblemCase.LibraryNotFound
import org.jetbrains.kotlin.library.loader.KlibLoaderResult.ProblematicLibrary
import java.io.File
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
    private val libraryProviders = ArrayList<KlibLibraryProvider>()
    private val libraryPaths = ArrayList<String>()
    private var platformChecker: KlibPlatformChecker? = null
    private var maxPermittedAbiVersion: KotlinAbiVersion? = null
    private var zipFileSystemAccessor: ZipFileSystemAccessor? = null
    private var manifestTransformer: KlibManifestTransformer? = null

    init {
        object : KlibLoaderSpec {
            override fun libraryProviders(providers: List<KlibLibraryProvider>) {
                libraryProviders += providers
            }

            override fun libraryProviders(vararg providers: KlibLibraryProvider) {
                libraryProviders += providers
            }

            override fun libraryPaths(paths: List<String>) {
                libraryPaths.addAll(paths)
            }

            override fun libraryPaths(vararg paths: String) {
                libraryPaths += paths
            }

            override fun libraryPaths(vararg paths: File) {
                paths.mapTo(libraryPaths) { it.path }
            }

            override fun libraryPaths(vararg paths: Path) {
                paths.mapTo(libraryPaths) { it.toString() }
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

            override fun manifestTransformer(transformer: KlibManifestTransformer) {
                manifestTransformer = transformer
            }
        }.init()
    }

    fun load(): KlibLoaderResult {
        // Convert all collected library paths to a library provider.
        val libraryProviders: List<KlibLibraryProvider> = buildList {
            this += DefaultKlibLibraryProvider(libraryPaths)
            this += libraryProviders
        }

        return KlibLoaderImpl(
            libraryProviders = libraryProviders,
            platformChecker = platformChecker,
            maxPermittedAbiVersion = maxPermittedAbiVersion,
            zipFileSystemAccessor = zipFileSystemAccessor ?: ZipFileSystemInPlaceAccessor,
            manifestTransformer = manifestTransformer
        ).loadLibraries()
    }
}

interface KlibLoaderSpec {
    fun libraryProviders(providers: List<KlibLibraryProvider>)
    fun libraryProviders(vararg providers: KlibLibraryProvider)

    fun libraryPaths(paths: List<String>)
    fun libraryPaths(vararg paths: String)
    fun libraryPaths(vararg paths: File)
    fun libraryPaths(vararg paths: Path)

    fun platformChecker(checker: KlibPlatformChecker)
    fun maxPermittedAbiVersion(abiVersion: KotlinAbiVersion)
    fun zipFileSystemAccessor(accessor: ZipFileSystemAccessor)

    fun manifestTransformer(transformer: KlibManifestTransformer)
}

private class KlibLoaderImpl(
    private val libraryProviders: List<KlibLibraryProvider>,
    private val platformChecker: KlibPlatformChecker?,
    private val maxPermittedAbiVersion: KotlinAbiVersion?,
    private val zipFileSystemAccessor: ZipFileSystemAccessor,
    private val manifestTransformer: KlibManifestTransformer?,
) {
    /**
     * This is needed to avoid inspecting the same raw paths multiple times.
     */
    private val visitedRawPaths = HashSet<String>()

    /**
     * And this is needed to avoid inspecting the same canonical paths more than once.
     *
     * Note that transformation of a raw library path, which actually can be any form of
     * a relative path, to a canonical path happens after checking that the path is valid
     * and points to an existing file system object. That is why we have to keep both
     * [visitedRawPaths] and [visitedCanonicalPaths] to have complete deduplication.
     *
     * N.B. The original order of paths is preserved!
     */
    private val visitedCanonicalPaths = LinkedHashMap<Path, LibraryStatus>()

    /**
     * Generally, all identified loading problems are stored in [visitedCanonicalPaths] as [LibraryStatus.FailedToLoad].
     * Except for one specific case: The library's raw path was not valid, and therefore it was not possible to
     * convert it to a canonical path and add the entry to [visitedCanonicalPaths].
     *
     * At the same time we need to provide the list of loading problems in [KlibLoaderResult.problematicLibraries]
     * in exactly the same order as they were encountered during the loading process.
     *
     * So, we have to keep all loading problems as a separate list.
     */
    private val problematicLibraries = ArrayList<ProblematicLibrary>()

    fun loadLibraries(): KlibLoaderResult {
        libraryProviders.forEach { libraryProvider -> loadLibrariesSuggestedByProvider(libraryProvider) }

        val loadedLibrariesStdlibFirst = ArrayList<KotlinLibrary>()

        visitedCanonicalPaths.values.forEach { status ->
            if (status is LibraryStatus.SuccessfullyLoaded) {
                val library = status.library

                // Stdlib is a special library. In many cases it should be the first to be deserialized
                // to make the necessary preparations (set up built-ins, etc.)
                // So, it's wise to explicitly set it to the first place even if sorting of libraries per se
                // is not the responsibility of [KlibLoader].
                if (library.isAnyPlatformStdlib)
                    loadedLibrariesStdlibFirst.add(0, library)
                else
                    loadedLibrariesStdlibFirst.add(library)

                // Post-process all successfully loaded libraries.
                status.suggestedByProviders.forEach { libraryProvider ->
                    libraryProvider.postProcessLoadedLibrary(library)
                }
            }
        }

        return KlibLoaderResult(loadedLibrariesStdlibFirst, problematicLibraries)
    }

    private fun loadLibrariesSuggestedByProvider(libraryProvider: KlibLibraryProvider) {
        val rawPathsToProcess = ArrayList<String>()

        // Collect all raw paths that have not been visited yet.
        libraryProvider.getLibraryPaths().forEach { rawPath ->
            if (visitedRawPaths.add(rawPath)) {
                rawPathsToProcess += rawPath
            } // else: already visited, skip it.
        }

        rawPathsToProcess.forEach { rawPath ->
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
                return@forEach
            }

            val canonicalPath: Path = validPath.toRealPath()

            when (val alreadyVisitedStatus = visitedCanonicalPaths[canonicalPath]) {
                is LibraryStatus.SuccessfullyLoaded -> {
                    // Has been already successfully loaded.
                    alreadyVisitedStatus.suggestedByProviders += libraryProvider // To keep track of all providers that suggested this path.
                }

                is LibraryStatus.FailedToLoad -> {
                    // Has been already seen and failed to load. No need to inspect it again.
                }

                null -> {
                    // Has not been seen yet. Try to load it.
                    val visitedStatus = loadSingleLibrary(rawPath, validPath)
                    visitedCanonicalPaths[canonicalPath] = visitedStatus

                    when (visitedStatus) {
                        is LibraryStatus.SuccessfullyLoaded -> {
                            // To keep track of all providers that suggested this path.
                            visitedStatus.suggestedByProviders += libraryProvider
                        }

                        is LibraryStatus.FailedToLoad -> {
                            problematicLibraries += visitedStatus.problem
                        }
                    }
                }
            }
        }
    }

    private fun loadSingleLibrary(rawPath: String, validPath: Path): LibraryStatus {
        val library = try {
            // Important: Initialization of a KlibImpl instance always triggers reading and parsing
            // of the manifest file. If the manifest, which is the essential part of KLIB, is not available
            // or is corrupted, an exception is thrown. We immediately treat such library as problematic.
            KlibImpl(
                location = KFile(validPath),
                zipFileSystemAccessor = zipFileSystemAccessor,
                manifestTransformer = manifestTransformer,
            )
        } catch (_: Exception) {
            return LibraryStatus.FailedToLoad(ProblematicLibrary(rawPath, InvalidLibraryFormat))
        }

        platformChecker?.check(library)?.let { platformCheckMismatch ->
            return LibraryStatus.FailedToLoad(ProblematicLibrary(rawPath, platformCheckMismatch))
        }

        if (maxPermittedAbiVersion != null) {
            val libraryAbiVersion: KotlinAbiVersion? = library.versions.abiVersion
            if (libraryAbiVersion == null || !libraryAbiVersion.isAtMost(maxPermittedAbiVersion)) {
                return LibraryStatus.FailedToLoad(
                    ProblematicLibrary(
                        libraryPath = rawPath,
                        problemCase = IncompatibleAbiVersion(
                            libraryVersions = library.versions,
                            minPermittedAbiVersion = null,
                            maxPermittedAbiVersion = maxPermittedAbiVersion
                        )
                    )
                )
            }
        }

        return LibraryStatus.SuccessfullyLoaded(library)
    }

    private sealed interface LibraryStatus {
        class SuccessfullyLoaded(
            val library: KotlinLibrary,
            val suggestedByProviders: LinkedHashSet<KlibLibraryProvider> = linkedSetOf(),
        ) : LibraryStatus

        class FailedToLoad(val problem: ProblematicLibrary) : LibraryStatus
    }
}
