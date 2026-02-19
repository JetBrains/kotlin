/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput
import java.io.File

/** Converts a [File] to a path of type [String] (and vice versa) for serialization of file paths in IC caches. */
interface FileToPathConverter {
    fun toPath(file: File): String
    fun toFile(path: String): File
    fun getFileDescriptor(): KeyDescriptor<File> = FileDescriptor(this)
}

object BasicFileToPathConverter : FileToPathConverter {
    override fun toPath(file: File): String = file.path
    override fun toFile(path: String): File = File(path)
}

/**
 * [KeyDescriptor] for a [File] which uses the given [pathConverter] for serialization of file paths.
 *
 * To support build cache relocatability, the user of this class can provide a [RelocatableFileToPathConverter] as the [pathConverter].
 */
private class FileDescriptor(private val pathConverter: FileToPathConverter) : KeyDescriptor<File> {

    override fun save(output: DataOutput, file: File) {
        StringExternalizer.save(output, pathConverter.toPath(file))
    }

    override fun read(input: DataInput): File {
        return pathConverter.toFile(StringExternalizer.read(input))
    }

    override fun getHashCode(file: File): Int {
        // It's important to use the pathConverter to get a possibly relocatable path first before computing the hash code because the
        // returned hash code affects the contents of IC caches.
        // For example, if we get the hash code directly without using pathConverter, the hash codes of the following files will be
        // different and therefore the contents of the IC caches will also be different (i.e., the IC caches will not be relocatable):
        //      File("/path/to/project1/src/foo.kt").hashCode() = 123
        //      File("/path/to/project2/src/foo.kt").hashCode() = 456
        // If we use a RelocatableFileToPathConverter instead, the file paths will be normalized first, so we'll get the same hash codes:
        //      [In "/path/to/project1"] File("src/foo.kt").hashCode() = 789
        //      [In "/path/to/project2"] File("src/foo.kt").hashCode() = 789
        return pathConverter.toPath(file).hashCode()
    }

    override fun isEqual(file1: File, file2: File): Boolean {
        return file1 == file2
    }
}
