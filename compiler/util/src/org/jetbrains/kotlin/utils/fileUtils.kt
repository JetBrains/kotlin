/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.utils.fileUtils

import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.pathString

fun File.withReplacedExtensionOrNull(oldExt: String, newExt: String): File? {
    if (name.endsWith(oldExt)) {
        val path = path
        val pathWithoutExt = path.substring(0, path.length - oldExt.length)
        val pathWithNewExt = pathWithoutExt + newExt
        return File(pathWithNewExt)
    }

    return null
}

fun Path.withReplacedExtensionOrNull(oldExt: String, newExt: String): Path? {
    if (name.endsWith(oldExt)) {
        val path = pathString
        val pathWithoutExt = path.substring(0, path.length - oldExt.length)
        val pathWithNewExt = pathWithoutExt + newExt
        return Path(pathWithNewExt)
    }

    return null
}

/**
 * Calculates the relative path to this file from [base] file.
 * Note that the [base] file is treated as a directory.
 *
 * If this file matches the [base] directory an empty path is returned.
 * If this file does not belong to the [base] directory, it is returned unchanged.
 */
fun File.descendantRelativeTo(base: File): File {
    assert(base.isAbsolute) { "$base" }
    assert(base.isDirectory) { "$base" }
    val cwd = base.normalize()
    val filePath = this.absoluteFile.normalize()
    return if (filePath.startsWith(cwd)) filePath.relativeTo(cwd) else this
}

/**
 * Try to resolve all symbolic links that might appear in any path segment.
 * In case of unresolvable symlinks (or any other type of [IOException]) return the unresolved absolute path.
 */
fun resolveSymlinksGracefully(rawPath: String): Path {
    // The "official" way of `Paths.get(path).toRealPath()` does not work on Windows
    //   if the path starts with `/C:` - `sun.nio.fs.WindowsPathParser.normalize`
    //   throws `InvalidPathException: Illegal char <:>`.
    val correctedPath: Path = if (System.getProperty("os.name").contains("Windows") && rawPath.startsWith("/"))
        Paths.get(rawPath.removePrefix("/"))
    else
        Paths.get(rawPath)

    return runCatching {
        // Try to resolve the path that might have a symlink in any path segment.
        correctedPath.toRealPath()
    }.recover { exception ->
        if (exception !is IOException) throw exception
        // In case of unresolvable symlinks (or any other IO error happened during the path resolution)
        // fall back to the original (unresolved) path.
        Paths.get(rawPath)
    }.getOrThrow()
}
