/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.properties

import org.jetbrains.kotlin.konan.file.*
import java.io.StringWriter

typealias Properties = java.util.Properties

fun File.loadProperties(): Properties {
    val properties = java.util.Properties()
    this.bufferedReader().use { reader ->
        properties.load(reader)
    }
    return properties
}

/**
 * Standard properties writer has two issues, which prevents build reproducibility
 *
 * 1. The order of lines is not defined
 * 2. It uses platform-specific end-of-lines
 *
 * This function deals with both issues
 */
fun File.saveProperties(properties: Properties) {
    val rawData = StringWriter().apply {
        properties.store(this, null)
    }.toString()

    val lines = rawData
        .split(System.lineSeparator())
        .filterNot { it.isEmpty() || it.startsWith("#") }
        .sorted()

    outputStream().use {
        it.write(lines.joinToString("\n", postfix = "\n").toByteArray())
    }
}

fun Properties.saveToFile(file: File) = file.saveProperties(this)
