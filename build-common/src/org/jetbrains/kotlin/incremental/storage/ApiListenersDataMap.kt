/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import com.intellij.util.io.EnumeratorStringDescriptor
import org.jetbrains.kotlin.incremental.dumpCollection

import org.jetbrains.kotlin.name.FqName
import java.io.File

class ApiListenersDataMap(
    storageFile: File,
    private val pathConverter: FileToPathConverter
) : BasicStringMap<Collection<String>>(storageFile, EnumeratorStringDescriptor(), StringCollectionExternalizer) {

    operator fun set(fqName: FqName, dependantFiles: Collection<File>) {
        storage[fqName.asString()] = dependantFiles.map(pathConverter::toPath)
    }

    operator fun get(fqName: FqName): Collection<File> =
        storage[fqName.asString()].orEmpty().map(pathConverter::toFile)

    override fun dumpValue(value: Collection<String>) =
        value.dumpCollection()

    fun remove(fqName: FqName): Collection<File> =
        get(fqName).also { storage.remove(fqName.asString()) }

    val keys: Collection<FqName>
        get() = storage.keys.map { FqName(it) }
}
