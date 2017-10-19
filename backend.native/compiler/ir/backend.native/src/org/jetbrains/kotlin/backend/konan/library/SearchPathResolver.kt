/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.library

import org.jetbrains.kotlin.backend.konan.Distribution
import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.backend.konan.util.removeSuffixIfPresent
import org.jetbrains.kotlin.backend.konan.util.suffixIfNot
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetManager

interface SearchPathResolver {
    val searchRoots: List<File>
    fun resolve(givenPath: String): File
    fun defaultLinks(nostdlib: Boolean, noDefaultLibs: Boolean): List<File>
}

fun defaultResolver(repositories: List<String>, targetManager: TargetManager): SearchPathResolver =
        defaultResolver(repositories, Distribution(targetManager))

fun defaultResolver(repositories: List<String>, distribution: Distribution): SearchPathResolver =
        KonanLibrarySearchPathResolver(
                repositories,
                distribution.targetManager,
                distribution.klib,
                distribution.localKonanDir
        )

fun SearchPathResolver.resolveImmediateLibraries(libraryNames: List<String>,
                                                 target: KonanTarget,
                                                 abiVersion: Int = 1,
                                                 noStdLib: Boolean = false,
                                                 noDefaultLibs: Boolean = false,
                                                 removeDuplicates: Boolean = true): List<LibraryReaderImpl> {

    val defaultLibraries = defaultLinks(nostdlib = noStdLib, noDefaultLibs = noDefaultLibs).map {
        LibraryReaderImpl(it, abiVersion, target, isDefaultLink = true)
    }

    val userProvidedLibraries = libraryNames
            .map { resolve(it) }
            .map{ LibraryReaderImpl(it, abiVersion, target) }

    val resolvedLibraries = defaultLibraries + userProvidedLibraries

    return resolvedLibraries.let {
        if (removeDuplicates) it.distinctBy { it.libraryFile.absolutePath } else it
    }
}

fun SearchPathResolver.resolveLibrariesRecursive(immediateLibraries: List<LibraryReaderImpl>,
                                                 target: KonanTarget,
                                                 abiVersion: Int) {
    val cache = mutableMapOf<File, LibraryReaderImpl>()
    cache.putAll(immediateLibraries.map { it.libraryFile.absoluteFile to it })
    var newDependencies = cache.values.toList()
    do {
        newDependencies = newDependencies.map { library: LibraryReaderImpl ->
            library.unresolvedDependencies
                    .map { resolve(it).absoluteFile }
                    .map { 
                        if (it in cache) {
                            library.resolvedDependencies.add(cache[it]!!)
                            null
                        } else {
                            val reader = LibraryReaderImpl(it, abiVersion, target)
                            cache.put(it,reader)
                            library.resolvedDependencies.add(reader) 
                            reader
                        }
            }.filterNotNull()
        } .flatten()
    } while (newDependencies.isNotEmpty())
}

fun List<LibraryReaderImpl>.withResolvedDependencies(): List<LibraryReaderImpl> {
    val result = mutableSetOf<LibraryReaderImpl>()
    result.addAll(this)
    var newDependencies = result.toList()
    do {
        newDependencies = newDependencies
            .map { it -> it.resolvedDependencies } .flatten()
            .filter { it !in result }
        result.addAll(newDependencies)
    } while (newDependencies.isNotEmpty())
    return result.toList()
}

fun SearchPathResolver.resolveLibrariesRecursive(libraryNames: List<String>,
                                                 target: KonanTarget,
                                                 abiVersion: Int = 1,
                                                 noStdLib: Boolean = false,
                                                 noDefaultLibs: Boolean = false): List<LibraryReaderImpl> {
    val immediateLibraries = resolveImmediateLibraries(
                    libraryNames = libraryNames,
                    target = target,
                    abiVersion = abiVersion,
                    noStdLib = noStdLib,
                    noDefaultLibs = noDefaultLibs,
                    removeDuplicates = true
            )
    resolveLibrariesRecursive(immediateLibraries, target, abiVersion)
    return immediateLibraries.withResolvedDependencies()
}

class KonanLibrarySearchPathResolver(
        repositories: List<String>,
        val targetManager: TargetManager?,
        val distributionKlib: String?,
        val localKonanDir: String?,
        val skipCurrentDir: Boolean = false
): SearchPathResolver {

    val localHead: File?
        get() = localKonanDir?.File()?.klib

    val distHead: File?
        get() = distributionKlib?.File()?.child("common")

    val distPlatformHead: File?
        get() = targetManager?.let { distributionKlib?.File()?.child("platform")?.child(targetManager.targetName) }

    val currentDirHead: File?
        get() = if (!skipCurrentDir) File.userDir else null

    private val repoRoots: List<File> by lazy {
        repositories.map{File(it)}
    }

    // This is the place where we specify the order of library search.
    override val searchRoots: List<File> by lazy {
        (listOf(currentDirHead) + repoRoots + listOf(localHead, distHead, distPlatformHead)).filterNotNull()
    }

    private fun found(candidate: File): File? {
        fun check(file: File): Boolean =
                file.exists && (file.isFile || File(file, "manifest").exists)

        val noSuffix = File(candidate.path.removeSuffixIfPresent(".klib"))
        val withSuffix = File(candidate.path.suffixIfNot(".klib"))
        return when {
            check(withSuffix) -> withSuffix
            check(noSuffix) -> noSuffix
            else -> null
        }
    }

    override fun resolve(givenPath: String): File {
        val given = File(givenPath)
        if (given.isAbsolute) {
            found(given)?.apply{ return this }
        } else {
            searchRoots.forEach{ 
                found(File(it, givenPath))?.apply{return this}
            }
        }
        error("Could not find \"$givenPath\" in ${searchRoots.map{it.absolutePath}}.")
    }

    private val File.klib
        get() = File(this, "klib")

    // The libraries from the default root are linked automatically.
    val defaultRoots: List<File>
        get() = listOf(distHead, distPlatformHead)
                .filterNotNull()
                .filter{ it.exists }

    override fun defaultLinks(nostdlib: Boolean, noDefaultLibs: Boolean): List<File> {
        val defaultLibs = defaultRoots.flatMap{ it.listFiles }
            .filterNot { it.name.removeSuffixIfPresent(".klib") == "stdlib" }
            .map { File(it.absolutePath) }
        val result = mutableListOf<File>()
        if (!nostdlib) result.add(resolve("stdlib"))
        if (!noDefaultLibs) result.addAll(defaultLibs)
        return result
    }
}

