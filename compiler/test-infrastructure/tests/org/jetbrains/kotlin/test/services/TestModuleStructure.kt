/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.container.topologicalSort
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.DependencyRelation.*
import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

abstract class TestModuleStructure : TestService {
    abstract val modules: List<TestModule>
    abstract val allDirectives: RegisteredDirectives
    abstract val originalTestDataFiles: List<File>
}

val TestServices.moduleStructure: TestModuleStructure by TestServices.testServiceAccessor()

// --------------------------------------- Transitive dependencies ---------------------------------------


/**
 * Example structure:
 * ```
 * // MODULE: a
 * // MODULE: b(a)
 * // MODULE: c(b)
 * ```
 *
 * Passed module: `c`
 *
 * Results:
 * |                     | reverseOrder = false | reverseOrder = true |
 * |---------------------|----------------------|---------------------|
 * | includeSelf = false | [b, a]               | [a, b]              |
 * | includeSelf = true  | [c, b, a]            | [a, b, c]           |
 */
fun TestModule.transitiveRegularDependencies(
    includeSelf: Boolean = false,
    reverseOrder: Boolean = false,
    filter: (DependencyDescription) -> Boolean = { true },
): List<TestModule> {
    return transitiveDependencies(RegularDependency, includeSelf = includeSelf, reverseOrder = reverseOrder, filter)
}

fun TestModule.transitiveFriendDependencies(
    includeSelf: Boolean = false,
    reverseOrder: Boolean = false,
    filter: (DependencyDescription) -> Boolean = { true },
): List<TestModule> {
    return transitiveDependencies(FriendDependency, includeSelf = includeSelf, reverseOrder = reverseOrder, filter)
}

fun TestModule.transitiveDependsOnDependencies(
    includeSelf: Boolean = false,
    reverseOrder: Boolean = false,
    filter: (DependencyDescription) -> Boolean = { true },
): List<TestModule> {
    return transitiveDependencies(DependsOnDependency, includeSelf = includeSelf, reverseOrder = reverseOrder, filter)
}

private fun TestModule.transitiveDependencies(
    expectedRelation: DependencyRelation,
    includeSelf: Boolean,
    reverseOrder: Boolean,
    filter: (DependencyDescription) -> Boolean,
): List<TestModule> {
    val result = topologicalSort(listOf(this), reverseOrder = reverseOrder) { item ->
        item.allDependencies.mapNotNull { dep ->
            dep.dependencyModule.takeIf { dep.relation == expectedRelation && filter(dep) }
        }
    }
    if (!includeSelf) {
        return result - this
    }
    return result
}

// --------------------------------------- Leaf modules ---------------------------------------

/**
 * @return true if there are no other modules that depend on the current one
 */
fun TestModule.isLeafModule(testServices: TestServices): Boolean {
    return isLeafModule(testServices, TestModule::allDependencies)
}

/**
 * @return true if there are no other modules that depend on the current one with dependsOn relation.
 * So no other module actualizes this one:
 *
 * ```
 * // MODULE: lib-common
 * // MODULE: lib-platform()()(lib-common)
 *
 * // MODULE: app-common(lib-common)
 * // MODULE: app-platform(lib-platform)()(app-common)
 * ```
 *
 * In this hierarchy `lib-platform` and `app-platform` are leaf modules in MPP graph, but
 * only `app-platform` is just a leaf module
 */
fun TestModule.isLeafModuleInMppGraph(testServices: TestServices): Boolean {
    return isLeafModule(testServices, TestModule::dependsOnDependencies)
}

private inline fun TestModule.isLeafModule(
    testServices: TestServices,
    dependencies: TestModule.() -> List<DependencyDescription>,
): Boolean {
    val targetModule = this
    return testServices.moduleStructure.modules.none {
        it != targetModule && targetModule in it.dependencies().map { it.dependencyModule }
    }
}
