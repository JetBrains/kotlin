/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.util

import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

fun Element.traverseParents(block: (Element) -> Unit) {
    block(this)
    parents.forEach { it.traverseParents(block) }
}

operator fun <K, V, U> MutableMap<K, MutableMap<V, U>>.set(k1: K, k2: V, value: U) {
    this.putIfAbsent(k1, mutableMapOf())
    val map = getValue(k1)
    map[k2] = value
}

operator fun <K, V, U> Map<K, Map<V, U>>.get(k1: K, k2: V): U {
    return getValue(k1).getValue(k2)
}

inline fun File.writeToFileUsingSmartPrinterIfFileContentChanged(block: SmartPrinter.() -> Unit) {
    val newText = buildString { SmartPrinter(this).block() }
    GeneratorsFileUtil.writeFileIfContentChanged(this, newText, logNotChanged = false)
}
