package org.jetbrains.kotlin.konan.library.resolver

import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.SearchPathResolverWithTarget
import org.jetbrains.kotlin.library.UnresolvedLibrary

interface KonanLibraryResolver {

    val searchPathResolver: SearchPathResolverWithTarget

    /**
     * Given the list of Kotlin/Native library names, ABI version and other parameters
     * resolves libraries and evaluates dependencies between them.
     */
    fun resolveWithDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean = false,
        noDefaultLibs: Boolean = false
    ): KonanLibraryResolveResult
}

interface KonanLibraryResolveResult {

    fun filterRoots(predicate: (KonanResolvedLibrary) -> Boolean): KonanLibraryResolveResult

    fun getFullList(order: LibraryOrder? = null): List<KonanLibrary>

    fun forEach(action: (KonanLibrary, PackageAccessedHandler) -> Unit)
}

typealias LibraryOrder = (Iterable<KonanResolvedLibrary>) -> List<KonanResolvedLibrary>

val TopologicalLibraryOrder: LibraryOrder = { input ->
    val sorted = mutableListOf<KonanResolvedLibrary>()
    val visited = mutableSetOf<KonanResolvedLibrary>()
    val tempMarks = mutableSetOf<KonanResolvedLibrary>()

    fun visit(node: KonanResolvedLibrary, result: MutableList<KonanResolvedLibrary>) {
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