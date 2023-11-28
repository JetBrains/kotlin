package org.jetbrains.kotlin.library.metadata.resolver

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
    ): KotlinLibraryResolveResult =
        resolveWithoutDependencies(
            unresolvedLibraries,
            noStdLib,
            noDefaultLibs,
            noEndorsedLibs
        ).resolveDependencies()

    fun resolveWithoutDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean = false,
        noDefaultLibs: Boolean = false,
        noEndorsedLibs: Boolean = false,
    ): List<KotlinLibrary>

    fun List<KotlinLibrary>.resolveDependencies(): KotlinLibraryResolveResult
}

interface KotlinLibraryResolveResult {

    fun filterRoots(predicate: (KotlinResolvedLibrary) -> Boolean): KotlinLibraryResolveResult

    fun getFullList(order: LibraryOrder? = null): List<KotlinLibrary> = getFullResolvedList(order).map { it.library }
    fun getFullResolvedList(order: LibraryOrder? = null): List<KotlinResolvedLibrary>

    fun forEach(action: (KotlinLibrary, PackageAccessHandler) -> Unit)
}


typealias LibraryOrder = (Iterable<KotlinResolvedLibrary>) -> List<KotlinResolvedLibrary>

val TopologicalLibraryOrder: LibraryOrder = { input ->
    val sorted = mutableListOf<KotlinResolvedLibrary>()
    val visited = mutableSetOf<KotlinResolvedLibrary>()
    val tempMarks = mutableSetOf<KotlinResolvedLibrary>()

    fun visit(node: KotlinResolvedLibrary, result: MutableList<KotlinResolvedLibrary>) {
        if (visited.contains(node)) return
        if (tempMarks.contains(node)) error("Cyclic dependency in library graph for: ${node.library.libraryName}")
        tempMarks.add(node)
        node.resolvedDependencies.forEach {
            visit(it, result)
        }
        visited.add(node)
        result += node
    }

    input.forEach next@{
        if (visited.contains(it)) return@next
        visit(it, sorted)
    }

    sorted
}
