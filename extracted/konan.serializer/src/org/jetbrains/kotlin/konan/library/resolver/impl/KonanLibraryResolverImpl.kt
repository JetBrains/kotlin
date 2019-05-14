/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

package org.jetbrains.kotlin.konan.library.resolver.impl

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.library.resolver.*
import org.jetbrains.kotlin.konan.util.WithLogger
import org.jetbrains.kotlin.library.UnresolvedLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies

internal class KonanLibraryResolverImpl(
        override val searchPathResolver: SearchPathResolverWithTarget
): KonanLibraryResolver, WithLogger by searchPathResolver {

    override fun resolveWithDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean,
        noDefaultLibs: Boolean
    ) = findLibraries(unresolvedLibraries, noStdLib, noDefaultLibs)
            .leaveDistinct()
            .resolveDependencies()

    /**
     * Returns the list of libraries based on [libraryNames], [noStdLib] and [noDefaultLibs] criteria.
     *
     * This method does not return any libraries that might be available via transitive dependencies
     * from the original library set (root set).
     */
    private fun findLibraries(
            unresolvedLibraries: List<UnresolvedLibrary>,
            noStdLib: Boolean,
            noDefaultLibs: Boolean
    ): List<KonanLibrary> {

        val userProvidedLibraries = unresolvedLibraries.asSequence()
                .map { searchPathResolver.resolve(it) }
                .toList()

        val defaultLibraries = searchPathResolver.defaultLinks(noStdLib, noDefaultLibs)

        // Make sure the user provided ones appear first, so that
        // they have precedence over defaults when duplicates are eliminated.
        return userProvidedLibraries + defaultLibraries
    }

    /**
     * Leaves only distinct libraries (by absolute path), warns on duplicated paths.
     */
    private fun List<KonanLibrary>.leaveDistinct() =
            this.groupBy { it.libraryFile.absolutePath }.let { groupedByAbsolutePath ->
                warnOnLibraryDuplicates(groupedByAbsolutePath.filter { it.value.size > 1 }.keys)
                groupedByAbsolutePath.map { it.value.first() }
            }

    private fun warnOnLibraryDuplicates(duplicatedPaths: Iterable<String>) {
        duplicatedPaths.forEach { logger.warning("library included more than once: $it") }
    }


    /**
     * Given the list of root libraries does the following:
     *
     * 1. Evaluates other libraries that are available via transitive dependencies.
     * 2. Wraps each [KonanLibrary] into a [KonanResolvedLibrary] with information about dependencies on other libraries.
     * 3. Creates resulting [KonanLibraryResolveResult] object.
     */
    private fun List<KonanLibrary>.resolveDependencies(): KonanLibraryResolveResult {

        val rootLibraries = this.map { KonanResolvedLibraryImpl(it) }

        // As far as the list of root libraries is known from the very beginning, the result can be
        // constructed from the very beginning as well.
        val result = KonanLibraryResolverResultImpl(rootLibraries)

        val cache = mutableMapOf<File, KonanResolvedLibrary>()
        cache.putAll(rootLibraries.map { it.library.libraryFile.absoluteFile to it })

        var newDependencies = rootLibraries
        do {
            newDependencies = newDependencies.map { library: KonanResolvedLibraryImpl ->
                library.library.unresolvedDependencies.asSequence()

                        .map { KonanResolvedLibraryImpl(searchPathResolver.resolve(it)) }
                        .map { resolved ->
                            val absoluteFile = resolved.library.libraryFile.absoluteFile
                            if (absoluteFile in cache) {
                                library.addDependency(cache[absoluteFile]!!)
                                null
                            } else {
                                cache.put(absoluteFile, resolved)
                                library.addDependency(resolved)
                                resolved
                            }

                        }.filterNotNull()
                        .toList()
            }.flatten()
        } while (newDependencies.isNotEmpty())

        return result
    }
}

internal class KonanLibraryResolverResultImpl(
        private val roots: List<KonanResolvedLibrary>
): KonanLibraryResolveResult {

    private val all: List<KonanResolvedLibrary> by lazy {
        val result = mutableSetOf<KonanResolvedLibrary>().also { it.addAll(roots) }

        var newDependencies = result.toList()
        do {
            newDependencies = newDependencies
                    .map { it -> it.resolvedDependencies }.flatten()
                    .filter { it !in result }
            result.addAll(newDependencies)
        } while (newDependencies.isNotEmpty())

        result.toList()
    }

    override fun filterRoots(predicate: (KonanResolvedLibrary) -> Boolean) =
            KonanLibraryResolverResultImpl(roots.filter(predicate))

    override fun getFullList(order: LibraryOrder?) = (order?.invoke(all) ?: all).asPlain()

    override fun forEach(action: (KonanLibrary, PackageAccessedHandler) -> Unit) {
        all.forEach { action(it.library, it) }
    }

    private fun List<KonanResolvedLibrary>.asPlain() = map { it.library }

    override fun toString() = "roots=$roots, all=$all"
}
