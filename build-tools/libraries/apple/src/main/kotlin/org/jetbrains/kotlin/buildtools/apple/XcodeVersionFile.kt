/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * The `.xcode-version` file that xcodes, xcenv and similar tools use to pin a project to an Xcode version.
 *
 * The library does not look for the file on its own: the integration decides which directory to start from.
 */
public object XcodeVersionFile {
    /** The file name, `.xcode-version`. */
    public const val FILE_NAME: String = ".xcode-version"

    /**
     * Reads the version spec from the file contents: the first line that is neither blank nor a `#` comment.
     * Returns `null` when there is no such line. Throws [IllegalArgumentException] when the line is not a valid spec.
     */
    @JvmStatic
    public fun parse(text: String): XcodeRequest.Version? =
        text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
            ?.let { XcodeRequest.Version(it) }

    /**
     * Looks for [FILE_NAME] in [startDirectory] and its parents, stopping after [stopAt] when given.
     * Returns the first file found, or `null`.
     */
    @JvmStatic
    public fun find(startDirectory: Path, stopAt: Path? = null): Path? {
        val stop = stopAt?.toAbsolutePath()?.normalize()
        var directory: Path? = startDirectory.toAbsolutePath().normalize()
        while (directory != null) {
            val candidate = directory.resolve(FILE_NAME)
            if (candidate.isRegularFile()) return candidate
            if (directory == stop) return null
            directory = directory.parent
        }
        return null
    }

    /**
     * Reads the version spec from [file]. Returns `null` when the file holds no version. Throws
     * [IllegalArgumentException] naming the file when it holds an invalid one.
     */
    @JvmStatic
    public fun read(file: Path): XcodeRequest.Version? = try {
        parse(file.readText())
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("$file: ${e.message}", e)
    }
}
