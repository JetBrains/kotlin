/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree.imports

import org.jetbrains.kotlin.generators.tree.TypeRef
import org.jetbrains.kotlin.generators.tree.TypeVariable
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.joinToWithBuffer

/**
 * Represents some context where types and other declarations can be added to the list of imports in the current file.
 */
interface ImportCollecting {

    fun addImport(importable: Importable)

    fun addStarImport(packageName: String) {
        addImport(ArbitraryImportable(packageName, "*"))
    }

    fun addAllImports(importables: Collection<Importable>) {
        importables.forEach(this::addImport)
    }

    object Empty : ImportCollecting {
        override fun addImport(importable: Importable) {}
        override fun addStarImport(packageName: String) {}
        override fun addAllImports(importables: Collection<Importable>) {}
    }

    /**
     * Prints this type as a string with all its arguments and question marks, while recursively collecting
     * `this` and other referenced types into this import collector.
     */
    fun TypeRef.render(): String = buildString { renderTo(this, this@ImportCollecting) }

    /**
     * The angle bracket-delimited list of type parameters to print, or empty string if the list is empty.
     *
     * For type parameters that have a single upper bound, also prints that upper bound. If at least one type parameter has multiple upper
     * bounds, doesn't print any upper bounds at all. They are expected to be printed in the `where` clause (see [multipleUpperBoundsList]).
     *
     * @param end The string to add after the closing angle bracket of the type parameter list
     */
    fun List<TypeVariable>.typeParameters(end: String = ""): String = buildString {
        if (this@typeParameters.isEmpty()) return@buildString
        joinToWithBuffer(this, prefix = "<", postfix = ">") { param ->
            if (param.variance != Variance.INVARIANT) {
                append(param.variance.label)
                append(" ")
            }
            append(param.name)
            param.bounds.singleOrNull()?.let {
                append(" : ")
                it.renderTo(this, this@ImportCollecting)
            }
        }
        append(end)
    }

    /**
     * The `where` clause to print after the class or function declaration if at least one of the element's tye parameters has multiple upper
     * bounds.
     *
     * Otherwise, an empty string.
     */
    fun List<TypeVariable>.multipleUpperBoundsList(): String {
        val paramsWithMultipleUpperBounds = filter { it.bounds.size > 1 }.takeIf { it.isNotEmpty() } ?: return ""
        return buildString {
            append(" where ")
            paramsWithMultipleUpperBounds.joinToWithBuffer(this, separator = ", ") { param ->
                param.bounds.joinToWithBuffer(this) { bound ->
                    append(param.name)
                    append(" : ")
                    bound.renderTo(this, this@ImportCollecting)
                }
            }
        }
    }
}