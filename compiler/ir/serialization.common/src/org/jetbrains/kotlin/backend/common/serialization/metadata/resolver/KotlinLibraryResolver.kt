package org.jetbrains.kotlin.Kotlin.library.resolver

import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary

interface KotlinLibraryResolver<L: KotlinLibrary> {

    val searchPathResolver: SearchPathResolver<L>

    /**
     * Given the list of Kotlin/Native library names, ABI version and other parameters
     * resolves libraries and evaluates dependencies between them.
     */
    fun resolveWithDependencies(
        unresolvedLibraries: List<UnresolvedLibrary>,
        noStdLib: Boolean = false,
        noDefaultLibs: Boolean = false
    ): KotlinLibraryResolveResult
}

interface KotlinLibraryResolveResult {

    fun filterRoots(predicate: (KotlinResolvedLibrary) -> Boolean): KotlinLibraryResolveResult

    fun getFullList(order: LibraryOrder? = null): List<KotlinLibrary>

    fun forEach(action: (KotlinLibrary, PackageAccessedHandler) -> Unit)
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