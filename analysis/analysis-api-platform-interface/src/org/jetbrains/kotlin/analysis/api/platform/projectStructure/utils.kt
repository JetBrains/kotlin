/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.projectStructure

import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaPlatformInterface
import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaScriptModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.psi.UserDataProperty

/**
 * Computes the transitive `dependsOn` dependencies of [directDependsOnDependencies]. [computeTransitiveDependsOnDependencies] is the
 * default computation strategy to provide [KaModule.transitiveDependsOnDependencies].
 *
 * The algorithm is a depth-first search-based topological sort. `dependsOn` dependencies cannot be cyclical and thus form a DAG, which
 * allows the application of a topological sort.
 */
@KaPlatformInterface
public fun computeTransitiveDependsOnDependencies(directDependsOnDependencies: List<KaModule>): List<KaModule> =
    topologicalSort(directDependsOnDependencies) { this.directDependsOnDependencies }

@KaPlatformInterface
@OptIn(KaExperimentalApi::class)
public fun KaModule.areCompilerPluginsSupported(): Boolean =
    when (this) {
        is KaSourceModule, is KaScriptModule -> true
        else -> false
    }

@KaPlatformInterface
@OptIn(KaExperimentalApi::class)
public fun KaModule.asDebugString(indent: Int = 0): String =
    buildString {
        appendLine("$moduleDescription {")
        appendLine("contentScope: $contentScope")
        appendLine("directRegularDependencies:${System.lineSeparator()}")
        directRegularDependencies.joinTo(this, separator = System.lineSeparator()) { dep ->
            dep.asDebugString(indent + 1)
        }
        appendLine("directDependsOnDependencies:${System.lineSeparator()}")
        directDependsOnDependencies.joinTo(this, separator = System.lineSeparator()) { dep ->
            dep.asDebugString(indent + 1)
        }
        append("}")
    }.prependIndent("  ".repeat(indent))

/**
 * Used by implementations of [KaResolveExtensionProvider][org.jetbrains.kotlin.analysis.api.resolve.extensions.KaResolveExtensionProvider]
 * to store a reference of the [KaModule] for which a [VirtualFile] was generated.
 */
@KaImplementationDetail
public var VirtualFile.resolveExtensionFileModule: KaModule? by UserDataProperty(Key.create("RESOLVE_EXTENSION_FILE_MODULE"))

/**
 * Sorts [nodes] topologically collecting direct edges via [dependencies]. [nodes] and [dependencies] must form a directed, acyclic graph.
 * [topologicalSort] will throw an [IllegalStateException] if it encounters a cycle.
 *
 * The algorithm is based on depth-first search, starting in order from each node in [nodes]. Kahn's algorithm is harder to apply to the
 * ad-hoc dependency structure because it's not easily apparent whether a node has no other incoming edges.
 *
 * For example, consider the following structure: `C -> A, C -> B, B -> A`. The resulting order should be `[C, B, A]`. However, `A` is
 * first in the list of dependencies of `C`. Without a way to find the incoming edge from `B` to `A` while processing `C -> A`, a naive
 * implementation of Kahn's algorithm might order `A` before `B`.
 */
private fun <A> topologicalSort(nodes: Iterable<A>, dependencies: A.() -> Iterable<A>): List<A> {
    val visiting = mutableSetOf<A>()
    val visited = mutableSetOf<A>()

    fun visit(node: A) {
        if (node in visited) return
        if (node in visiting) error("Cannot compute a topological sort: The node $node is in a cycle.")

        // Keeping track of the nodes that are being visited allows the algorithm to throw an exception in case of a cycle. The input should
        // never be cyclic, but this approach gives some additional safety in case of bugs.
        visiting.add(node)
        node.dependencies().forEach(::visit)
        visiting.remove(node)

        visited.add(node)
    }

    nodes.forEach(::visit)

    // The paper algorithm inserts nodes at the head of the result list. Because our `visited` set remembers elements in their order of
    // insertion, the result needs to be reversed.
    return visited.toMutableList().apply { reverse() }
}
