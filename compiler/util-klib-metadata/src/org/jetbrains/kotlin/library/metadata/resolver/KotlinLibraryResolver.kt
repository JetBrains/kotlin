package org.jetbrains.kotlin.library.metadata.resolver

import org.jetbrains.kotlin.config.DuplicatedUniqueNameStrategy
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SearchPathResolver
import org.jetbrains.kotlin.library.UnresolvedLibrary
import org.jetbrains.kotlin.library.metadata.PackageAccessHandler

interface KotlinLibraryResolver<L : KotlinLibrary> {

    val searchPathResolver: SearchPathResolver<L>

    /**
     * Given the list of Kotlin/Native library names, ABI version and other parameters
     * resolves libraries and evaluates dependencies between them.
     */
    fun resolveWithDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean = false,
        noDefaultLibs: Boolean = false,
        noEndorsedLibs: Boolean = false,
        duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy = DuplicatedUniqueNameStrategy.DENY,
    ): KotlinLibraryResolveResult =
        resolveWithoutDependencies(
            unresolvedLibraries,
            noStdLib,
            noDefaultLibs,
            noEndorsedLibs,
            duplicatedUniqueNameStrategy,
        ).resolveDependencies()

    @Deprecated("Restored to keep ABI compatibility with kotlinx-benchmark Gradle plugin (KT-71414)", level = DeprecationLevel.HIDDEN)
    fun resolveWithDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean = false,
        noDefaultLibs: Boolean = false,
        noEndorsedLibs: Boolean = false,
    ): KotlinLibraryResolveResult =
        resolveWithDependencies(
            unresolvedLibraries,
            noStdLib,
            noDefaultLibs,
            noEndorsedLibs,
            DuplicatedUniqueNameStrategy.DENY
        )

    fun resolveWithoutDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean = false,
        noDefaultLibs: Boolean = false,
        noEndorsedLibs: Boolean = false,
        duplicatedUniqueNameStrategy: DuplicatedUniqueNameStrategy,
    ): List<KotlinLibrary>

    fun List<KotlinLibrary>.resolveDependencies(): KotlinLibraryResolveResult
}

interface KotlinLibraryResolveResult {

    fun filterRoots(predicate: (KotlinResolvedLibrary) -> Boolean): KotlinLibraryResolveResult

    fun getFullList(): List<KotlinLibrary>

    fun forEach(action: (KotlinLibrary, PackageAccessHandler) -> Unit)
}
