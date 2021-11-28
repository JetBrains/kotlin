/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.util

import java.io.File

private const val TAGS_FILE_NAME = "_tags.txt"
private val PROHIBITED_SYMBOLS = listOf(' ', ',', '(', ')', '&', '|', '!')

fun extractTagsFromDirectory(dir: File): List<String> {
    require(dir.isDirectory) {
        "${dir.absolutePath} is not a directory"
    }
    val tagsFile = dir.resolve(TAGS_FILE_NAME)
    if (!tagsFile.exists()) return emptyList()
    return tagsFile.readLines().filter { it.isNotBlank() }.onEach(::validateTag)
}

// TODO: support tags in testdata files
fun extractTagsFromTestFile(@Suppress("UNUSED_PARAMETER") file: File): List<String> = emptyList()

private fun validateTag(tag: String) {
    require(PROHIBITED_SYMBOLS.none { it in tag }) {
        "Tag \"tag\" contains one of prohibited symbols: $PROHIBITED_SYMBOLS"
    }
}
