/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm

// JLS 3.9 Keywords, plus the reserved literals `true`, `false` and `null` (JLS 3.10.3, 3.10.8)
// and the reserved identifier `_` (JLS 3.8).
// Contextual keywords (`record`, `yield`, `sealed`, `permits`, `var`, ...) are deliberately absent:
// they are valid identifiers in Java.
private val JAVA_KEYWORDS = setOf(
    "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
    "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
    "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
    "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
    "volatile", "while",
    "true", "false", "null",
    "_",
)

/**
 * Whether [name] can be used as an identifier in Java source code.
 */
fun isValidJavaIdentifier(name: String): Boolean {
    if (name.isEmpty()) return false
    if (name in JAVA_KEYWORDS) return false

    var index = 0
    val firstCodePoint = name.codePointAt(index)
    if (!Character.isJavaIdentifierStart(firstCodePoint)) return false
    index += Character.charCount(firstCodePoint)

    while (index < name.length) {
        val codePoint = name.codePointAt(index)
        if (!Character.isJavaIdentifierPart(codePoint)) return false
        index += Character.charCount(codePoint)
    }
    return true
}
