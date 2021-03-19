/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.imltogradle

import org.jdom.Element
import org.jdom.input.SAXBuilder
import java.io.File

fun String.trimMarginWithInterpolations(): String {
    val regex = Regex("""^(\s*\|)(\s*).*$""")
    val out = mutableListOf<String>()
    var prevIndent = ""
    for (line in lines()) {
        val matchResult = regex.matchEntire(line)
        if (matchResult != null) {
            out.add(line.removePrefix(matchResult.groupValues[1]))
            prevIndent = matchResult.groupValues[2]
        } else {
            out.add(prevIndent + line)
        }
    }
    return out.joinToString("\n").trim()
}

fun File.readXml(): Element {
    return inputStream().use { SAXBuilder().build(it).rootElement }
}

fun Element.traverseChildren(): Sequence<Element> {
    suspend fun SequenceScope<Element>.visit(element: Element) {
        element.children.forEach { visit(it) }
        yield(element)
    }
    return sequence { visit(this@traverseChildren) }
}

inline fun <reified T> Any?.safeAs(): T? {
    return this as? T
}
