/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting

class PrintableAnnotation(
    private val classRef: ClassRef<*>,
    private val arguments: Map<String, String> = emptyMap()
) {
    context(importCollecting: ImportCollecting)
    val asClassRefString: String get() = with(importCollecting) { classRef.render() + "::class" }

    context(importCollecting: ImportCollecting)
    fun render(withAtSymbol: Boolean = true): String {
        return with(importCollecting) {
            val firstSymbol = if (withAtSymbol) "@" else ""
            val prefix = "$firstSymbol${classRef.render()}"
            if (arguments.isEmpty()) return@with prefix
            "$prefix(${arguments.entries.joinToString { [name, value] -> "$name = $value" }})"
        }
    }

    fun withArgument(name: String, value: String) = PrintableAnnotation(classRef, arguments + (name to value))
}

fun ClassRef<*>.toAnnotation(): PrintableAnnotation = PrintableAnnotation(this)
