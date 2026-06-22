/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.file

import org.jetbrains.kotlin.io.unzipTo
import org.jetbrains.kotlin.io.zipDirAsInternal

fun File.zipDirAs(zipFile: File): Unit = zipDirAsInternal(dirPath = this.javaPath, zipFilePath = zipFile.javaPath)

/**
 * Unpacks the contents of a zip archive located in [this] into the [destinationDirectory].
 *
 * @param destinationDirectory The directory to unpack the contents to.
 * @param resetTimeAttributes Whether to set the newly created files' time attributes
 * (creation time, last access time, and last modification time) to zero.
 * @param fromSubdirectory A subdirectory inside the archive to unpack. Specify "/" if you need to unpack the whole archive.
 */
fun File.unzipTo(destinationDirectory: File, fromSubdirectory: File = File("/"), resetTimeAttributes: Boolean = false) {
    javaPath.unzipTo(destinationDirectory.javaPath, fromSubdirectory.javaPath, resetTimeAttributes)
}
