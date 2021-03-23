/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import org.jdom.Element
import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.module.JpsDependencyElement
import org.jetbrains.jps.model.module.JpsLibraryDependency
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleDependency
import java.util.*
import kotlin.Comparator
import kotlin.collections.HashSet

data class JpsDependencyDescriptor(val moduleOrLibrary: Either<JpsModule, JpsLibrary>, val scope: JpsJavaDependencyScope) {
    companion object {
        fun from(dep: JpsDependencyElement, moduleImlRootElement: Element?): JpsDependencyDescriptor? {
            val moduleOrLibrary = when (dep) {
                is JpsModuleDependency -> dep.module
                    .orElse { error("Cannot resolve module reference = ${dep.moduleReference}") }
                    .let { Either.First(it) }
                is JpsLibraryDependency -> dep.resolve(moduleImlRootElement)
                    .orElse { error("Cannot resolve library reference = ${dep.libraryReference}") }
                    .let { Either.Second(it) }
                else -> null
            } ?: return null
            return JpsDependencyDescriptor(moduleOrLibrary, dep.scope)
        }
    }
}

fun JpsModule.flattenExportedTransitiveDependencies(
    initialScope: JpsJavaDependencyScope,
    jpsDependencyToDependantModuleIml: (JpsDependencyElement) -> Element?
): Sequence<JpsDependencyDescriptor> {
    val visitedModuleNames = HashSet<String>()
    val toVisit = PriorityQueue<JpsDependencyDescriptor>(compareBy(JpsDependencyScopeCompileClasspathComparator.reversed()) { it.scope })

    // Dijkstra's algorithm: intersect is like sum
    suspend fun SequenceScope<JpsDependencyDescriptor>.visit(current: JpsDependencyDescriptor) {
        when (val moduleOrLibrary = current.moduleOrLibrary) {
            is Either.First -> {
                val jpsModule = moduleOrLibrary.value
                if (!visitedModuleNames.add(jpsModule.name)) {
                    return
                }
                yield(current)
                val elements = jpsModule.dependencies
                    .filter { it.isExported }
                    .mapNotNull { JpsDependencyDescriptor.from(it, jpsDependencyToDependantModuleIml(it)) }
                    .map { JpsDependencyDescriptor(it.moduleOrLibrary, it.scope intersect current.scope) }
                toVisit.addAll(elements)
                while (toVisit.isNotEmpty()) {
                    visit(toVisit.poll())
                }
            }
            is Either.Second -> yield(current)
        }
    }
    return sequence { visit(JpsDependencyDescriptor(Either.First(this@flattenExportedTransitiveDependencies), initialScope)) }
}

private object JpsDependencyScopeCompileClasspathComparator : Comparator<JpsJavaDependencyScope> {
    private val ascending = listOf(
        JpsJavaDependencyScope.RUNTIME,
        JpsJavaDependencyScope.TEST,
        JpsJavaDependencyScope.PROVIDED,
        JpsJavaDependencyScope.COMPILE,
    )

    override fun compare(first: JpsJavaDependencyScope?, second: JpsJavaDependencyScope?): Int {
        val firstIndex = ascending.indexOf(first).takeIf { it != -1 } ?: error("Unknown $first")
        val secondIndex = ascending.indexOf(second).takeIf { it != -1 } ?: error("Unknown $second")
        return firstIndex.compareTo(secondIndex)
    }
}

private infix fun JpsJavaDependencyScope.intersect(other: JpsJavaDependencyScope): JpsJavaDependencyScope {
    return minOf(this, other, JpsDependencyScopeCompileClasspathComparator)
}
