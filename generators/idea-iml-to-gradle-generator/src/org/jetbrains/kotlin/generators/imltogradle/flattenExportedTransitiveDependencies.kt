/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import org.jetbrains.jps.model.java.JpsJavaDependencyScope
import org.jetbrains.jps.model.library.JpsLibrary
import org.jetbrains.jps.model.module.JpsDependencyElement
import org.jetbrains.jps.model.module.JpsLibraryDependency
import org.jetbrains.jps.model.module.JpsModule
import org.jetbrains.jps.model.module.JpsModuleDependency
import java.util.*

data class JpsDependencyDescriptor(val moduleOrLibrary: Either<JpsModule, JpsLibrary>, val scope: JpsJavaDependencyScope) {
    companion object {
        fun from(dep: JpsDependencyElement): JpsDependencyDescriptor? {
            val moduleOrLibrary = when (dep) {
                is JpsModuleDependency -> dep.module
                    .orElse { error("Cannot resolve module reference = ${dep.moduleReference}") }
                    .let { Either.First(it) }
                is JpsLibraryDependency -> dep.library
                    .orElse { error("Cannot resolve library reference = ${dep.libraryReference}") }
                    .let { Either.Second(it) }
                else -> return null
            }
            return JpsDependencyDescriptor(moduleOrLibrary, dep.scope)
        }
    }
}

fun JpsModule.flattenExportedTransitiveDependencies(): Sequence<JpsDependencyDescriptor> {
    val visitedModuleNames = HashSet<String>()
    val toVisit = PriorityQueue<JpsDependencyDescriptor>(
        compareBy<JpsDependencyDescriptor, JpsJavaDependencyScope>(JpsDependencyScopeCompileClasspathComparator.reversed()) { it.scope }
            .thenComparing(compareBy { it.moduleOrLibrary.value.name })
    )

    // Dijkstra's modified algorithm: intersectCompileClasspath is like sum
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
                    .map { JpsDependencyDescriptor.from(it)!! }
                    .map { it.copy(scope = it.scope intersectCompileClasspath current.scope) }
                toVisit.addAll(elements)
                while (toVisit.isNotEmpty()) {
                    visit(toVisit.poll())
                }
            }
            is Either.Second -> yield(current)
        }
    }
    return sequence {
        visit(
            JpsDependencyDescriptor(
                Either.First(this@flattenExportedTransitiveDependencies),
                JpsDependencyScopeCompileClasspathComparator.ascending.last()
            )
        )
    }
}

private object JpsDependencyScopeCompileClasspathComparator : Comparator<JpsJavaDependencyScope> {
    // Let's assume that module A depends on module B (A->B notation)
    // A[src] notation means "source root" of module A. A[test] notation means "test root" of module A.
    // Keeping this in mind let's consider different types of dependencies:
    val ascending = listOf(
        JpsJavaDependencyScope.RUNTIME,  // Doesn't establish any compile time dependencies
        JpsJavaDependencyScope.TEST,     // Compile time dependencies are: A[test]->B[src], A[test]->B[test] (so TEST > RUNTIME)
        JpsJavaDependencyScope.PROVIDED, // Compile time dependencies are like in TEST + A[src]->B[src] (so PROVIDED > TEST)
        JpsJavaDependencyScope.COMPILE,  /* Compile time dependencies are like in PROVIDED
                                            (so it actually doesn't matter in which order PROVIDED and COMPILE go) */
    )

    override fun compare(first: JpsJavaDependencyScope?, second: JpsJavaDependencyScope?): Int {
        val firstIndex = ascending.indexOf(first).takeIf { it != -1 } ?: error("Unknown $first")
        val secondIndex = ascending.indexOf(second).takeIf { it != -1 } ?: error("Unknown $second")
        return firstIndex.compareTo(secondIndex)
    }
}

infix fun JpsJavaDependencyScope.intersectCompileClasspath(other: JpsJavaDependencyScope): JpsJavaDependencyScope {
    return minOf(this, other, JpsDependencyScopeCompileClasspathComparator)
}
