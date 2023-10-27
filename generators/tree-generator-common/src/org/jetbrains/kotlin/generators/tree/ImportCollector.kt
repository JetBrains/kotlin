/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import java.util.SortedMap
import java.util.SortedSet

/**
 * Used to collect [TypeRef]s for printing a list of imports for those [TypeRef]s.
 */
class ImportCollector(currentPackage: String) {

    companion object {
        private val STAR = sortedSetOf("*")

        /**
         * The maximum number of imports from a single package before collapsing those imports to a star-import.
         */
        private const val STAR_COLLAPSE_THRESHOLD = 4
    }

    /**
     * A map of package names to a list of entities to import from that package.
     */
    private val imports: SortedMap<String, SortedSet<String>> = sortedMapOf()

    /**
     * Entities from these packages will not be imported explicitly.
     *
     * See [the list of default imports](https://kotlinlang.org/docs/packages.html#default-imports).
     */
    private val ignoredPackages = hashSetOf(
        currentPackage,
        "kotlin",
        "kotlin.annotation",
        "kotlin.collections",
        "kotlin.comparisons",
        "kotlin.io",
        "kotlin.ranges",
        "kotlin.sequences",
        "kotlin.text",
        "java.lang",
        "kotlin.jvm",
    )

    private fun addImport(packageName: String, entity: String) {
        if (packageName in ignoredPackages) return
        val entities = imports.computeIfAbsent(packageName) { sortedSetOf() }
        if (entities === STAR) return
        if (entity == "*") {
            imports[packageName] = STAR
            return
        }
        entities.add(entity)
        if (entities.size > STAR_COLLAPSE_THRESHOLD) {
            imports[packageName] = STAR
        }
    }

    fun addImport(importable: Importable) {
        addImport(importable.packageName, importable.typeName)
    }

    fun addStarImport(packageName: String) {
        addImport(packageName, "*")
    }

    /**
     * Prints all the collected imports in alphabetical order.
     *
     * @return `true` if at least one import was printed, `false` if no imports were printed.
     */
    fun printAllImports(printer: Appendable): Boolean {
        var atLeastOneImport = false
        for ((packageName, entities) in imports) {
            for (entity in entities) {
                atLeastOneImport = true
                printer.append("import ", packageName, ".", entity, "\n")
            }
        }
        return atLeastOneImport
    }

    fun addAllImports(importables: Collection<Importable>) {
        importables.forEach(this::addImport)
    }
}