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

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.konan.library.SearchPathResolver
import org.jetbrains.kotlin.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.konan.target.KonanTarget

const val KONAN_CURRENT_ABI_VERSION = 1

/**
 * Returns the list of [KonanLibraryReader]s given the list of user provided [libraryNames] along with
 * other parameters: [target], [abiVersion], [noStdLib], [noDefaultLibs].
 */
fun SearchPathResolver.resolveImmediateLibraries(libraryNames: List<String>,
                                                 target: KonanTarget,
                                                 abiVersion: Int = KONAN_CURRENT_ABI_VERSION,
                                                 noStdLib: Boolean = false,
                                                 noDefaultLibs: Boolean = false,
                                                 logger: ((String) -> Unit)?): List<KonanLibraryReader> {
    val userProvidedLibraries = libraryNames
            .map { resolve(it) }
            .map{ LibraryReaderImpl(it, abiVersion, target) }

    val defaultLibraries = defaultLinks(noStdLib = noStdLib, noDefaultLibs = noDefaultLibs).map {
        LibraryReaderImpl(it, abiVersion, target, isDefaultLibrary = true)
    }

    // Make sure the user provided ones appear first, so that 
    // they have precedence over defaults when duplicates are eliminated.
    val resolvedLibraries = userProvidedLibraries + defaultLibraries

    warnOnLibraryDuplicates(resolvedLibraries.map { it.libraryFile }, logger)

    return resolvedLibraries.distinctBy { it.libraryFile.absolutePath }
}

private fun warnOnLibraryDuplicates(resolvedLibraries: List<File>, logger: ((String) -> Unit)? ) {

    if (logger == null) return

    val duplicates = resolvedLibraries.groupBy { it.absolutePath } .values.filter { it.size > 1 }

    duplicates.forEach {
        logger("library included more than once: ${it.first().absolutePath}")
    }
}

/**
 * For each of the given [immediateLibraries] fills in `resolvedDependencies` field with the
 * [KonanLibraryReader]s the library !!directly!! depends on.
 */
fun SearchPathResolver.resolveLibrariesRecursive(immediateLibraries: List<KonanLibraryReader>,
                                                 target: KonanTarget,
                                                 abiVersion: Int) {
    val cache = mutableMapOf<File, KonanLibraryReader>()
    cache.putAll(immediateLibraries.map { it.libraryFile.absoluteFile to it })
    var newDependencies = cache.values.toList()
    do {
        newDependencies = newDependencies.map { library: KonanLibraryReader ->
            library.unresolvedDependencies
                    .map { resolve(it).absoluteFile }
                    .mapNotNull {
                        if (it in cache) {
                            library.resolvedDependencies.add(cache[it]!!)
                            null
                        } else {
                            val reader = LibraryReaderImpl(it, abiVersion, target)
                            cache[it] = reader
                            library.resolvedDependencies.add(reader)
                            reader
                        }
            }
        } .flatten()
    } while (newDependencies.isNotEmpty())
}

/**
 * For the given list of [KonanLibraryReader]s returns the list of [KonanLibraryReader]s
 * that includes the same libraries plus all their (transitive) dependencies.
 */
fun List<KonanLibraryReader>.withResolvedDependencies(): List<KonanLibraryReader> {
    val result = mutableSetOf<KonanLibraryReader>()
    result.addAll(this)
    var newDependencies = result.toList()
    do {
        newDependencies = newDependencies
            .map { it -> it.resolvedDependencies }.flatten()
            .filter { it !in result }
        result.addAll(newDependencies)
    } while (newDependencies.isNotEmpty())
    return result.toList()
}

fun SearchPathResolver.resolveLibrariesRecursive(libraryNames: List<String>,
                                                 target: KonanTarget,
                                                 abiVersion: Int = KONAN_CURRENT_ABI_VERSION,
                                                 noStdLib: Boolean = false,
                                                 noDefaultLibs: Boolean = false): List<KonanLibraryReader> {
    val immediateLibraries = resolveImmediateLibraries(
                    libraryNames = libraryNames,
                    target = target,
                    abiVersion = abiVersion,
                    noStdLib = noStdLib,
                    noDefaultLibs = noDefaultLibs,
                    logger = null
            )
    resolveLibrariesRecursive(immediateLibraries, target, abiVersion)
    return immediateLibraries.withResolvedDependencies()
}
