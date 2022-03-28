/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import org.jetbrains.kotlin.incremental.dumpCollection
import java.io.File

class ReflektDependencyMap(
    storageFile: File,
    private val pathConverter: FileToPathConverter
) : BasicStringMap<Collection<String>>(storageFile, PathStringDescriptor, StringCollectionExternalizer) {

    operator fun set(sourceFile: File, dependentFiles: Collection<File>) {
        storage[pathConverter.toPath(sourceFile)] = pathConverter.toPaths(dependentFiles)
    }

    operator fun get(sourceFile: File): Collection<File> {
        val paths = storage[pathConverter.toPath(sourceFile)].orEmpty()
        return pathConverter.toFiles(paths)
    }

    override fun dumpValue(value: Collection<String>) =
        value.dumpCollection()

    fun remove(file: File): Collection<File> =
        get(file).also { storage.remove(pathConverter.toPath(file)) }

    fun isEmpty() = storage.keys.isEmpty()

    fun getAllFiles(): List<File> {
        val allReflektFiles = mutableListOf<File>()
        for (key in storage.keys) {
            allReflektFiles.add(pathConverter.toFile(key))
            val paths = storage[key].orEmpty()
            allReflektFiles.addAll(pathConverter.toFiles(paths))
        }
        return allReflektFiles
    }
}