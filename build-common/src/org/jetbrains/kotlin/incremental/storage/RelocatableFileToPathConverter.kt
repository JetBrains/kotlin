/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import java.io.File

/**
 * [FileToPathConverter] which converts a [File] to a Unix-style relative path, which is relative to a base directory ([baseDir]), and vice
 * versa.
 *
 * This is to support Gradle build cache relocatability
 * (https://docs.gradle.org/current/userguide/build_cache_concepts.html#relocatability).
 */
class RelocatableFileToPathConverter(private val baseDir: File) : FileToPathConverter {

    private val unixStyleBaseDirPathPrefix = "${baseDir.invariantSeparatorsPath}/"

    override fun toPath(file: File): String {
        check(file.invariantSeparatorsPath.startsWith(unixStyleBaseDirPathPrefix)) {
            "The given file '${file.path}' is located outside the base directory '${baseDir.path}'"
        }
        return file.relativeTo(baseDir).invariantSeparatorsPath
    }

    override fun toFile(path: String): File {
        // Note: The given path is Unix-style but baseDir could be Windows-style; call normalize() so that the style of the returned file's
        // path is consistent with baseDir.
        return baseDir.resolve(path).normalize()
    }
}

/**
 * File locations to support Gradle build cache relocatability (see [RelocatableFileToPathConverter]). These include:
 *   - The root directory (or root directories) of the source files. Ideally, they should be the most specific root directories
 *   (e.g., `/path/to/MyApplication/app/src/main/kotlin`), but for simplicity we're using the root project directory ([rootProjectDir])
 *   for now (e.g., `/path/to/MyApplication`).
 *   - The root directory of the compiled class files ([classesDir]).
 */
class FileLocations(
    val rootProjectDir: File,
    val classesDir: File,
)
