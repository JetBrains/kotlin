/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration.klib

object KotlinNativeLibraryNameUtil {
    internal const val KOTLIN_NATIVE_LIBRARY_PREFIX = "Kotlin/Native"
    internal const val KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE = "$KOTLIN_NATIVE_LIBRARY_PREFIX "
    internal const val GRADLE_LIBRARY_PREFIX = "Gradle: "

    private val IDE_LIBRARY_NAME_REGEX = Regex("^$KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE([^\\s]+) - ([^\\s]+)( \\[([\\w]+)\\])?$")

    // Builds the name of Kotlin/Native library that is a part of Kotlin/Native distribution
    // as it will be displayed in IDE UI.
    fun buildIDELibraryName(kotlinVersion: String, libraryName: String, platform: String?): String {
        val platformNamePart = platform?.let { " [$it]" }.orEmpty()
        return "$KOTLIN_NATIVE_LIBRARY_PREFIX_PLUS_SPACE$kotlinVersion - $libraryName$platformNamePart"
    }

    // N.B. Returns null if this is not IDE name of Kotlin/Native library.
    fun parseIDELibraryName(ideLibraryName: String): Triple<String, String, String?>? {
        val match = IDE_LIBRARY_NAME_REGEX.matchEntire(ideLibraryName) ?: return null

        val kotlinVersion = match.groups[1]!!.value
        val libraryName = match.groups[2]!!.value
        val platform = match.groups[4]?.value

        return Triple(kotlinVersion, libraryName, platform)
    }

    fun isGradleLibraryName(ideLibraryName: String) = ideLibraryName.startsWith(GRADLE_LIBRARY_PREFIX)
}
