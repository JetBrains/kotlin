/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("NameRenderingUtils")

package org.jetbrains.kotlin.name

import org.jetbrains.kotlin.renderer.KeywordStringsGenerated

fun Name.render(stipSpecialMarkers: Boolean = false): String {
    val string = if (stipSpecialMarkers) asStringStripSpecialMarkers() else asString()
    return if ((!stipSpecialMarkers || !isSpecial) && shouldBeEscaped(string)) '`' + string + '`' else string
}

private fun shouldBeEscaped(string: String): Boolean {
    return string in KeywordStringsGenerated.KEYWORDS ||
            string.any { !Character.isLetterOrDigit(it) && it != '_' } ||
            string.isEmpty() ||
            !Character.isJavaIdentifierStart(string.codePointAt(0))
}

fun FqNameUnsafe.render(): String {
    return renderFqName(pathSegments())
}

fun FqName.render(): String {
    return renderFqName(pathSegments())
}

private fun renderFqName(pathSegments: List<Name>): String {
    return buildString {
        for (element in pathSegments) {
            if (isNotEmpty()) {
                append(".")
            }
            append(element.render())
        }
    }
}