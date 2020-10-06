/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("CommonVariableAsmNameManglingUtils")
package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.resolve.jvm.checkers.isValidDalvikCharacter

fun mangleNameIfNeeded(name: String): String {
    if (name.all { it.isValidCharacter() }) {
        return name
    }

    return buildString {
        for (c in name) {
            if (c.isValidCharacter()) {
                append(c)
            } else {
                val hexString = Integer.toHexString(c.toInt())
                assert(hexString.length <= 4)
                append("_u").append(hexString)
            }
        }
    }
}

private fun Char.isValidCharacter(): Boolean {
    return this != '$' && this != '-' && isValidDalvikCharacter(this)
}
