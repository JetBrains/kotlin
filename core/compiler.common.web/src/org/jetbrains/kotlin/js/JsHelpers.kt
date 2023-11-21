/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js

fun validateQualifier(qualifier: String): Boolean {
    val parts = qualifier.split('.')
    if (parts.isEmpty()) return false

    return parts.all { part ->
        part.isNotEmpty() && part[0].isJavaIdentifierStart() && part.drop(1).all(Char::isJavaIdentifierPart)
    }
}
