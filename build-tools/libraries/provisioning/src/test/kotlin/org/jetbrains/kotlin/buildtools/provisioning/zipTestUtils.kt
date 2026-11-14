/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.provisioning

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Writes a zip with [entries] in the given order: a name ending with '/' is a directory, anything else a file with that content. */
internal fun writeZip(zip: Path, vararg entries: Pair<String, String>): Path {
    ZipOutputStream(Files.newOutputStream(zip)).use { output ->
        for ((name, content) in entries) {
            output.putNextEntry(ZipEntry(name))
            if (!name.endsWith("/")) output.write(content.toByteArray())
            output.closeEntry()
        }
    }
    return zip
}

internal fun Path.readText(): String = String(Files.readAllBytes(this))
