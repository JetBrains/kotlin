/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.roots.libraries.Library

class ExternalLibraryInfo(
    val artifactId: String,
    val version: String
)

fun parseExternalLibraryName(library: Library): ExternalLibraryInfo? {
    val libName = library.name ?: return null

    val versionWithKind = libName.substringAfterLastNullable(":") ?: return null
    val version = versionWithKind.substringBefore("@")

    val artifactId = libName.substringBeforeLastNullable(":")?.substringAfterLastNullable(":") ?: return null

    if (version.isBlank() || artifactId.isBlank()) return null

    return ExternalLibraryInfo(artifactId, version)
}

private fun String.substringBeforeLastNullable(delimiter: String, missingDelimiterValue: String? = null): String? {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(0, index)
}

private fun String.substringAfterLastNullable(delimiter: String, missingDelimiterValue: String? = null): String? {
    val index = lastIndexOf(delimiter)
    return if (index == -1) missingDelimiterValue else substring(index + 1, length)
}
