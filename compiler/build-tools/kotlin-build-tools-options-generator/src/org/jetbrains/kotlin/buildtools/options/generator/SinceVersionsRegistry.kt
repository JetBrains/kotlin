/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.options.generator

import com.squareup.kotlinpoet.ClassName
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

internal class SinceVersionsRegistry(private val registryFile: Path) {

    private val versions: MutableMap<String, String> by lazy { loadFromFile() }

    private fun loadFromFile(): MutableMap<String, String> {
        if (!registryFile.exists()) return mutableMapOf()

        return registryFile.readLines()
            .filter { it.isNotBlank() && !it.startsWith('#') }
            .associate { line ->
                val idx = line.indexOf('=')
                line.substring(0, idx).trim() to line.substring(idx + 1).trim()
            }.toMutableMap()
    }

    fun getOrPut(className: ClassName, default: String): String =
        versions.getOrPut(className.canonicalName) { default }

    fun writeToFile() {
        val content = versions.entries
            .sortedBy { it.key }
            .joinToString("\n") { (k, v) -> "$k=$v" } + "\n"
        registryFile.writeText(content)
    }
}