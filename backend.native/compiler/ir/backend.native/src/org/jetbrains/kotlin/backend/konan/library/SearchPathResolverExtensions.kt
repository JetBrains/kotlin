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
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.SearchPathResolver
import org.jetbrains.kotlin.konan.library.createKonanLibraryReader
import org.jetbrains.kotlin.konan.library.unresolvedDependencies
import org.jetbrains.kotlin.konan.target.KonanTarget

const val KONAN_CURRENT_ABI_VERSION = 1

/**
 * Returns the list of [KonanLibrary]s given the list of user provided [libraryNames] along with
 * other parameters: [target], [abiVersion], [noStdLib], [noDefaultLibs].
 */
fun SearchPathResolver.resolveImmediateLibraries(libraryNames: List<String>,
                                                 target: KonanTarget,
                                                 abiVersion: Int = KONAN_CURRENT_ABI_VERSION,
                                                 noStdLib: Boolean = false,
                                                 noDefaultLibs: Boolean = false,
                                                 logger: ((String) -> Unit)?): List<KonanLibrary> {
    val userProvidedLibraries = libraryNames
            .map { resolve(it) }
            .map{ createKonanLibraryReader(it, abiVersion, target) }

    val defaultLibraries = defaultLinks(noStdLib = noStdLib, noDefaultLibs = noDefaultLibs).map {
        createKonanLibraryReader(it, abiVersion, target, isDefaultLibrary = true)
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
 * [KonanLibrary]s the library !!directly!! depends on.
 */
fun SearchPathResolver.resolveLibrariesRecursive(immediateLibraries: List<KonanLibrary>,
                                                 target: KonanTarget,
                                                 abiVersion: Int) {
    val cache = mutableMapOf<File, KonanLibrary>()
    cache.putAll(immediateLibraries.map { it.libraryFile.absoluteFile to it })
    var newDependencies = cache.values.toList()
    do {
        newDependencies = newDependencies.map { library: KonanLibrary ->
            library.unresolvedDependencies
                    .map { resolve(it).absoluteFile }
                    .mapNotNull {
                        if (it in cache) {
                            library.resolvedDependencies.add(cache[it]!!)
                            null
                        } else {
                            val reader = createKonanLibraryReader(it, abiVersion, target)
                            cache[it] = reader
                            library.resolvedDependencies.add(reader)
                            reader
                        }
            }
        } .flatten()
    } while (newDependencies.isNotEmpty())
}

/**
 * For the given list of [KonanLibrary]s returns the list of [KonanLibrary]s
 * that includes the same libraries plus all their (transitive) dependencies.
 */
fun List<KonanLibrary>.withResolvedDependencies(): List<KonanLibrary> {
    val result = mutableSetOf<KonanLibrary>()
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
                                                 noDefaultLibs: Boolean = false): List<KonanLibrary> {
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
