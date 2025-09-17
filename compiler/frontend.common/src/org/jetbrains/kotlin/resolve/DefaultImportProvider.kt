/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.name.FqName

abstract class DefaultImportProvider {
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
}
