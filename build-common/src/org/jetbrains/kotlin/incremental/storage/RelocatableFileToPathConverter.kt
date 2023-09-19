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
 * This is to support build cache relocatability
 * (https://docs.gradle.org/current/userguide/build_cache_concepts.html#relocatability).
 */
class RelocatableFileToPathConverter(private val baseDir: File) : FileToPathConverter {

    override fun toPath(file: File): String {
        // Note: If the given file is located outside `baseDir`, the relative path will start with "../". It's not "clean", but it can work.
        // TODO: Re-design the code such that `baseDir` always contains the given file (also add a precondition check here).
        return file.relativeTo(baseDir).invariantSeparatorsPath
    }

    override fun toFile(path: String): File {
        // Note: The given path is Unix-style but baseDir could be Windows-style; call normalize() so that the style of the returned file's
        // path is consistent with baseDir.
        return baseDir.resolve(path).normalize()
    }
}

/**
 * File locations to support build cache relocatability (see [RelocatableFileToPathConverter]).
 *
 * These are the root directories of
 *   - Source files
 *   - Output files, which include .class files and possibly additional output files such as `.java` stub files for KaptGenerateStubs tasks.
 *
 * Ideally, they  should be the most specific root directories (e.g., `/path/to/MyApplication/app/src/main/kotlin` for source files and
 * `/path/to/MyApplication/app/build/tmp/kotlin-classes/debug` for output .class files). However, for simplicity we are using the root
 * project directory ([rootProjectDir]) for source files and ([buildDir]) for output files.
 */
class FileLocations(
    val rootProjectDir: File,
    val buildDir: File,
) {

    fun getRelocatablePathConverterForSourceFiles() = RelocatableFileToPathConverter(rootProjectDir)

    fun getRelocatablePathConverterForOutputFiles() = RelocatableFileToPathConverter(buildDir)
}
