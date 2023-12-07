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

package org.jetbrains.kotlin.library.metadata.resolver.impl

import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolver
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.metadata.resolver.LibraryOrder
import org.jetbrains.kotlin.util.WithLogger

fun <L : KotlinLibrary> SearchPathResolver<L>.libraryResolver(resolveManifestDependenciesLenient: Boolean = false) =
    KotlinLibraryResolverImpl<L>(this, resolveManifestDependenciesLenient)

class KotlinLibraryResolverImpl<L : KotlinLibrary> internal constructor(
    override val searchPathResolver: SearchPathResolver<L>,
    val resolveManifestDependenciesLenient: Boolean,
) : KotlinLibraryResolver<L>, WithLogger by searchPathResolver {
    override fun resolveWithoutDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean,
        noDefaultLibs: Boolean,
        noEndorsedLibs: Boolean,
    ) = findLibraries(unresolvedLibraries, noStdLib, noDefaultLibs, noEndorsedLibs)
        .leaveDistinct()
        .omitDuplicateNames()

    /**
     * Returns the list of libraries based on [libraryNames], [noStdLib], [noDefaultLibs] and [noEndorsedLibs] criteria.
     *
     * This method does not return any libraries that might be available via transitive dependencies
     * from the original library set (root set).
     */
    private fun findLibraries(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean,
        noDefaultLibs: Boolean,
        noEndorsedLibs: Boolean,
    ): List<KotlinLibrary> {

        val userProvidedLibraries = unresolvedLibraries.asSequence()
            .mapNotNull { searchPathResolver.resolve(it) }
            .toList()

        val defaultLibraries = searchPathResolver.defaultLinks(noStdLib, noDefaultLibs, noEndorsedLibs)

        // Make sure the user provided ones appear first, so that
        // they have precedence over defaults when duplicates are eliminated.
        return userProvidedLibraries + defaultLibraries
    }

    /**
     * Leaves only distinct libraries (by absolute path).
     */
    private fun List<KotlinLibrary>.leaveDistinct(): List<KotlinLibrary> {
        if (size <= 1) return this

        val deduplicatedLibraries: Map<String, List<KotlinLibrary>> = groupByTo(linkedMapOf()) { it.libraryFile.absolutePath }
        return deduplicatedLibraries.values.map { it.first() }
    }

    /**
     * Having two libraries with the same `unique_name` we only keep the first one.
     *
     * TODO: This is actually undesirable behavior.
     *  - In certain situations it harms, e.g. KT-63573
     *  - But sometimes it is really necessary, e.g. KT-64115
     *  - Overall, we should not do any resolve inside the compiler (such as skipping KLIBs that happen to have repeated `unique_name`).
     *    This is an opaque process which better should be performed by the build system (e.g. Gradle). To be fixed in KT-64169
     */
    private fun List<KotlinLibrary>.omitDuplicateNames() =
        groupBy { it.uniqueName }.let { groupedByUniqName ->
            val librariesWithDuplicatedUniqueNames = groupedByUniqName.filterValues { it.size > 1 }
            librariesWithDuplicatedUniqueNames.entries.sortedBy { it.key }.forEach { (uniqueName, libraries) ->
                val libraryPaths = libraries.map { it.libraryFile.absolutePath }.sorted().joinToString()
                logger.warning("KLIB resolver: The same 'unique_name=$uniqueName' found in more than one library: $libraryPaths")
            }
            groupedByUniqName.map { it.value.first() } // This line is the reason of such issues as KT-63573.
        }

    /**
     * Given the list of root libraries does the following:
     *
     * 1. Evaluates other libraries that are available via transitive dependencies.
     * 2. Wraps each [KotlinLibrary] into a [KotlinResolvedLibrary] with information about dependencies on other libraries.
     * 3. Creates resulting [KotlinLibraryResolveResult] object.
     */
    override fun List<KotlinLibrary>.resolveDependencies(): KotlinLibraryResolveResult {
        val rootLibraries = this.map { KotlinResolvedLibraryImpl(it) }

        // As far as the list of root libraries is known from the very beginning, the result can be
        // constructed from the very beginning as well.
        val result = KotlinLibraryResolverResultImpl(rootLibraries)

        val cache = mutableMapOf<Any, KotlinResolvedLibrary>()
        cache.putAll(rootLibraries.map { it.library.libraryFile.fileKey to it })

        var newDependencies = rootLibraries
        do {
            newDependencies = newDependencies.map { library: KotlinResolvedLibraryImpl ->
                library.library.unresolvedDependencies(resolveManifestDependenciesLenient).asSequence()

                    .filterNot { searchPathResolver.isProvidedByDefault(it) }
                    .mapNotNull { searchPathResolver.resolve(it)?.let(::KotlinResolvedLibraryImpl) }
                    .map { resolved ->
                        val fileKey = resolved.library.libraryFile.fileKey
                        if (fileKey in cache) {
                            library.addDependency(cache[fileKey]!!)
                            null
                        } else {
                            cache.put(fileKey, resolved)
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

class KotlinLibraryResolverResultImpl(
    private val roots: List<KotlinResolvedLibrary>,
) : KotlinLibraryResolveResult {

    private val all: List<KotlinResolvedLibrary>
            by lazy {
                val result = mutableSetOf<KotlinResolvedLibrary>().also { it.addAll(roots) }

                var newDependencies = result.toList()
                do {
                    newDependencies = newDependencies
                        .map { it.resolvedDependencies }.flatten()
                        .filter { it !in result }
                    result.addAll(newDependencies)
                } while (newDependencies.isNotEmpty())

                result.toList()
            }

    override fun filterRoots(predicate: (KotlinResolvedLibrary) -> Boolean) =
        KotlinLibraryResolverResultImpl(roots.filter(predicate))

    override fun getFullResolvedList(order: LibraryOrder?) = (order?.invoke(all) ?: all)

    override fun forEach(action: (KotlinLibrary, PackageAccessHandler) -> Unit) {
        all.forEach { action(it.library, it) }
    }

    override fun toString() = "roots=$roots, all=$all"
}
