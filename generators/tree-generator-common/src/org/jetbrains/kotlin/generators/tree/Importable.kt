/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

interface Importable {
    val type: String
    val packageName: String?
    val fullQualifiedName: String? get() = packageName?.let { "$it.${type.replace(Regex("<.+>"), "")}" }

    fun getTypeWithArguments(notNull: Boolean = false): String
}

val Importable.typeWithArguments: String get() = getTypeWithArguments()