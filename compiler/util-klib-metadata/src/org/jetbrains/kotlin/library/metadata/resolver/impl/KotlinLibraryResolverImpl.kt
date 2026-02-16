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

import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolver
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.util.WithLogger
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@Deprecated(
    "Preserved for binary compatibility with existing versions of the kotlinx-benchmarks Gradle plugin. See KT-82882." +
            "\nPlease use KlibLoader.load() instead.",
    replaceWith = ReplaceWith("KlibLoader.load()", imports = ["org.jetbrains.kotlin.library.loader.KlibLoader"]),
    level = DeprecationLevel.ERROR
)
@JvmName("libraryResolver")
fun <L : KotlinLibrary> SearchPathResolver<L>.libraryResolverLegacy(resolveManifestDependenciesLenient: Boolean = false) =
    KotlinLibraryResolverImpl(
        this,
        resolveManifestDependenciesLenient,
        legacyExternalToolResolveMode = true
    )

@JvmName("libraryResolverNew")
fun <L : KotlinLibrary> SearchPathResolver<L>.libraryResolver(resolveManifestDependenciesLenient: Boolean = false) =
    KotlinLibraryResolverImpl(
        this,
        resolveManifestDependenciesLenient,
        legacyExternalToolResolveMode = false
    )

/**
 * @property searchPathResolver The resolver that looks up libraries by their paths.
 * @property resolveManifestDependenciesLenient Whether to resolve manifest dependencies leniently.
 * @property legacyExternalToolResolveMode Whether to use legacy external tool resolution mode. See KT-82882.
 */
class KotlinLibraryResolverImpl<L : KotlinLibrary> internal constructor(
    override val searchPathResolver: SearchPathResolver<L>,
    val resolveManifestDependenciesLenient: Boolean,
    private val legacyExternalToolResolveMode: Boolean,
) : KotlinLibraryResolver<L>, WithLogger by searchPathResolver {
    override fun resolveWithoutDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean,
        noDefaultLibs: Boolean,
        noEndorsedLibs: Boolean,
        duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy,
    ) = findLibraries(unresolvedLibraries, noStdLib, noDefaultLibs, noEndorsedLibs)
        .leaveDistinct()
        .omitDuplicateNames(duplicatedUniqueNameStrategy)

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

        val legacyExternalToolResolveModeLibraries = runIf(legacyExternalToolResolveMode) {
            (searchPathResolver as? KotlinLibraryProperResolverWithAttributes)
                ?.directLibs
                ?.mapNotNull { libraryPath ->
                    searchPathResolver.resolve(LenientUnresolvedLibrary(libraryPath))
                }
        }.orEmpty()

        val defaultLibraries = searchPathResolver.defaultLinks(noStdLib, noDefaultLibs, noEndorsedLibs)

        // Make sure the user provided ones appear first, so that
        // they have precedence over defaults when duplicates are eliminated.
        return userProvidedLibraries + legacyExternalToolResolveModeLibraries + defaultLibraries
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
    private fun List<KotlinLibrary>.omitDuplicateNames(duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy) : List<KotlinLibrary> {
        val deduplicatedLibs = groupBy { it.uniqueName }.let { groupedByUniqName ->
            val librariesWithDuplicatedUniqueNames = groupedByUniqName.filterValues { it.size > 1 }
            librariesWithDuplicatedUniqueNames.entries.sortedBy { it.key }.forEach { (uniqueName, libraries) ->
                val libraryPaths = libraries.map { it.libraryFile.absolutePath }.sorted().joinToString()
                val message = "KLIB resolver: The same 'unique_name=$uniqueName' found in more than one library: $libraryPaths"
                if (duplicatedUniqueNameStrategy == DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING ||
                    duplicatedUniqueNameStrategy == DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING
                ) {
                    logger.strongWarning(message)
                } else {
                    logger.error(
                        message + "\n" +
                                "Please file an issue to https://kotl.in/issue and meanwhile use CLI flag `-Xklib-duplicated-unique-name-strategy` with one of the following values:\n" +
                                "${DuplicatedUniqueNameStrategy.ALLOW_ALL_WITH_WARNING}: Use all KLIB dependencies, even when they have same `unique_name` property.\n" +
                                "${DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING}: Use the first KLIB dependency with clashing `unique_name` property. No order guarantees are given though.\n" +
                                "${DuplicatedUniqueNameStrategy.DENY}: Fail a compilation with the error."
                    )
                }
            }
            groupedByUniqName.map { it.value.first() } // This line is the reason of such issues as KT-63573.
        }
        return if (duplicatedUniqueNameStrategy == DuplicatedUniqueNameStrategy.ALLOW_FIRST_WITH_WARNING)
            deduplicatedLibs
        else this
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

        val processingQueue = ArrayDeque(rootLibraries)
        while(processingQueue.isNotEmpty()) {
            val currentLibrary = processingQueue.removeFirst()
            currentLibrary.library.unresolvedDependencies(resolveManifestDependenciesLenient)
                .forEach { unresolvedDependency ->
                    if (!searchPathResolver.isProvidedByDefault(unresolvedDependency)) {
                        searchPathResolver.resolve(unresolvedDependency)?.let { resolvedDependencyLibrary ->
                            val fileKey = resolvedDependencyLibrary.libraryFile.fileKey
                            if (fileKey in cache) {
                                currentLibrary.addDependency(cache[fileKey]!!)
                            } else {
                                val newlyResolved = KotlinResolvedLibraryImpl(resolvedDependencyLibrary)
                                cache[fileKey] = newlyResolved
                                currentLibrary.addDependency(newlyResolved)
                                processingQueue.add(newlyResolved)
                            }
                        }
                    }
                }
        }
        return result
    }
}

class KotlinLibraryResolverResultImpl(
    private val roots: List<KotlinResolvedLibrary>,
) : KotlinLibraryResolveResult {

    private val all: List<KotlinResolvedLibrary>
            by lazy {
                val result = mutableSetOf<KotlinResolvedLibrary>()
                val queue = ArrayDeque(roots) // Use a queue for traversal
                result.addAll(roots)

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    for (dependency in current.resolvedDependencies) {
                        if (result.add(dependency)) {
                            queue.add(dependency)
                        }
                    }
                }
                result.toList()
            }

    override fun filterRoots(predicate: (KotlinResolvedLibrary) -> Boolean) =
        KotlinLibraryResolverResultImpl(roots.filter(predicate))

    override fun getFullList(): List<KotlinLibrary> = all.map { it.library }

    override fun forEach(action: (KotlinLibrary, PackageAccessHandler) -> Unit) {
        all.forEach { action(it.library, it) }
    }

    override fun toString() = "roots=$roots, all=$all"
}
