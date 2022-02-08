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

fun File.withReplacedExtensionOrNull(oldExt: String, newExt: String): File? {
    if (name.endsWith(oldExt)) {
        val path = path
        val pathWithoutExt = path.substring(0, path.length - oldExt.length)
        val pathWithNewExt = pathWithoutExt + newExt
        return File(pathWithNewExt)
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
