/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("DalvikIdentifierUtils")
package org.jetbrains.kotlin.resolve.jvm.checkers

fun isValidDalvikIdentifier(identifier: String) = identifier.all { isValidDalvikCharacter(it) }

// https://source.android.com/devices/tech/dalvik/dex-format.html#string-syntax
fun isValidDalvikCharacter(c: Char) = when (c) {
    in 'A'..'Z' -> true
    in 'a'..'z' -> true
    in '0'..'9' -> true
    '$', '-', '_' -> true
    in '\u00a1' .. '\u1fff' -> true
    in '\u2010' .. '\u2027' -> true
    in '\u2030' .. '\ud7ff' -> true
    in '\ue000' .. '\uffef' -> true
    else -> false
}