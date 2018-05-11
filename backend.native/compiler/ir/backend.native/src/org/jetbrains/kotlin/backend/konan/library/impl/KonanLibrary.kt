/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.library.impl

import org.jetbrains.kotlin.backend.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.util.removeSuffixIfPresent
import org.jetbrains.kotlin.konan.file.*
import org.jetbrains.kotlin.konan.target.KonanTarget

open class ZippedKonanLibrary(val klibFile: File, override val target: KonanTarget? = null): KonanLibrary {
    init {
        if (!klibFile.exists) {
            error("Could not find $klibFile.")
        }
        if (!klibFile.isFile) {
            error("Expected $klibFile to be a regular file.")
        }
    }

    override val libraryName = klibFile.path.removeSuffixIfPresent(".klib")

    override val libDir by lazy {
       klibFile.asZipRoot
    }

    fun unpackTo(newDir: File) {
        if (newDir.exists) {
            if (newDir.isDirectory) 
                newDir.deleteRecursively()
            else 
                newDir.delete()
        }
        libDir.recursiveCopyTo(newDir)
        if (!newDir.exists) error("Could not unpack $klibFile as $newDir.")
    }
}

// This class automatically extracts pieces of 
// the library on first access. Use it if you need
// to pass extracted files to an external tool.
// Otherwise, stick to ZippedKonanLibrary.
class FileExtractor(zippedLibrary: KonanLibrary): KonanLibrary by zippedLibrary {

    override val manifestFile: File by lazy {
        extract(super.manifestFile)
    }

    override val resourcesDir: File by lazy {
        extractDir(super.resourcesDir)
    }

    override val includedDir: File by lazy {
        extractDir(super.includedDir)
    }

    override val kotlinDir: File by lazy {
        extractDir(super.kotlinDir)
    }

    override val nativeDir: File by lazy {
        extractDir(super.nativeDir)
    }

    override val linkdataDir: File by lazy {
        extractDir(super.linkdataDir)
    }

    fun extract(file: File): File {
        val temporary = createTempFile(file.name)
        file.copyTo(temporary)
        temporary.deleteOnExit()
        return temporary
    }

    fun extractDir(directory: File): File {
        val temporary = createTempDir(directory.name)
        directory.recursiveCopyTo(temporary)
        temporary.deleteOnExitRecursively()
        return temporary
    }
}

class UnzippedKonanLibrary(override val libDir: File, override val target: KonanTarget? = null): KonanLibrary {
    override val libraryName = libDir.path

    val targetList: List<String> by lazy {
        targetsDir.listFiles.map{it.name}
    }
}

fun KonanLibrary(klib: File, target: KonanTarget? = null) = 
    if (klib.isFile) ZippedKonanLibrary(klib, target) 
    else UnzippedKonanLibrary(klib, target)

val KonanLibrary.realFiles 
    get() = when (this) {
        is ZippedKonanLibrary -> FileExtractor(this)
        // Unpacked library just provides its own files.
        is UnzippedKonanLibrary -> this 
        else -> error("Provide an extractor for your container.")
    }

