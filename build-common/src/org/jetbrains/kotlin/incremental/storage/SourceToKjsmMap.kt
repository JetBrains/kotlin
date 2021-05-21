/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.storage

import com.intellij.util.io.EnumeratorStringDescriptor
import java.io.File

class SourceToKjsmMap(storageFile: File, private val pathConverter: FileToPathConverter) :
    BasicStringMap<String>(storageFile, EnumeratorStringDescriptor.INSTANCE) {

    operator fun set(sourceFile: File, kjsmFile: File) {
        storage[pathConverter.toPath(sourceFile)] = pathConverter.toPath(kjsmFile)
    }

    fun get(sourceFile: File): String? =
        storage[pathConverter.toPath(sourceFile)]

    fun remove(sourceFile: File) {
        storage.remove(pathConverter.toPath(sourceFile))
    }

    override fun dumpValue(value: String): String = value
}