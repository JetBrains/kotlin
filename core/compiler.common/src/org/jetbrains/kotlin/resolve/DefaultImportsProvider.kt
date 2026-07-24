/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.FqName

abstract class DefaultImportsProvider {
    private val defaultImports: List<ImportPath> = listOf(
        "kotlin.*",
        "kotlin.annotation.*",
        "kotlin.collections.*",
        "kotlin.ranges.*",
        "kotlin.sequences.*",
        "kotlin.text.*",
        "kotlin.io.*",
        "kotlin.comparisons.*",
    ).map { ImportPath.fromString(it) }

    abstract val platformSpecificDefaultImports: List<ImportPath>
    open val defaultLowPriorityImports: List<ImportPath> get() = emptyList()

    open val excludedImports: List<FqName> get() = emptyList()

    fun getDefaultImports(includeLowPriorityImports: Boolean): List<ImportPath> {
        return buildList {
            addAll(defaultImports)
            addAll(platformSpecificDefaultImports)
            if (includeLowPriorityImports) {
                addAll(defaultLowPriorityImports)
            }
        }
    }

    class Composed(val providers: List<DefaultImportsProvider>) : DefaultImportsProvider() {
        override val platformSpecificDefaultImports: List<ImportPath> by lazy {
            providers.map { it.platformSpecificDefaultImports }
                .reduce<_, Collection<ImportPath>> { acc, list -> acc.intersect(list) }
                .toList()
        }

        override val defaultLowPriorityImports: List<ImportPath> by lazy {
            providers.map { it.defaultLowPriorityImports }
                .reduce { acc, list -> acc + list }
                .distinct()
        }

        override val excludedImports: List<FqName> by lazy {
            providers.map { it.excludedImports }
                .reduce { acc, list -> acc + list }
                .distinct()
        }
    }
}
