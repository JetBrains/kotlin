/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.io

import java.io.StringWriter
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.bufferedReader
import kotlin.io.path.bufferedWriter

/**
 * Reads a properties file by the given [Path].
 */
fun Path.readProperties(): Properties {
    val properties = Properties()
    bufferedReader().use { reader -> properties.load(reader) }
    return properties
}

/**
 * Writes the given [properties] to the file.
 * - No comments are written.
 * - All properties are stored in the alphabetical order by their names.
 */
fun Path.writeProperties(properties: Properties) {
    val rawPropertiesText: String = StringWriter().apply { properties.store(this, null) }.toString()

    val filteredPropertiesText = rawPropertiesText
        .split(System.lineSeparator())
        .filterNot { it.isBlank() || it.startsWith("#") }
        .sorted()
        .joinToString("\n", postfix = "\n")

    bufferedWriter().use { writer -> writer.write(filteredPropertiesText) }
}
