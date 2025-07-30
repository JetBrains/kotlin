/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.build.report.debug
import org.jetbrains.kotlin.incremental.storage.AbstractBasicMap
import org.jetbrains.kotlin.incremental.storage.BasicMapsOwner
import org.jetbrains.kotlin.incremental.storage.IntExternalizer
import org.jetbrains.kotlin.incremental.storage.ListExternalizer
import org.jetbrains.kotlin.incremental.storage.toDescriptor
import java.io.DataInput
import java.io.DataOutput
import java.io.File

class CompilerPluginFilesCache(
    workingDir: File,
    private val icContext: IncrementalCompilationContext,
) : BasicMapsOwner(workingDir) {
    companion object {
        private const val SOURCES_REFERENCED_BY_PLUGINS = "sources-referenced-by-plugins"
        private const val OUTPUTS_GENERATED_FOR_PLUGINS = "outputs-generated-for-plugins"
    }

    private val sourceFilesReferencedByPlugins: ListLikeMap = registerMap(
        ListLikeMap(
            storageFile = SOURCES_REFERENCED_BY_PLUGINS.storageFile,
            fileDescriptor = icContext.fileDescriptorForSourceFiles,
            icContext = icContext
        )
    )

    private val outputFilesGeneratedForPlugins: ListLikeMap = registerMap(
        ListLikeMap(
            storageFile = OUTPUTS_GENERATED_FOR_PLUGINS.storageFile,
            fileDescriptor = icContext.fileDescriptorForOutputFiles,
            icContext = icContext
        )
    )

    fun getSourceFilesReferencedByPlugins(): List<File> {
        return sourceFilesReferencedByPlugins.getValue()
    }

    fun recordSourceFilesReferencedByPlugins(files: List<File>) {
        sourceFilesReferencedByPlugins.setValue(files)
    }

    fun recordOutputFilesGeneratedByPlugins(files: List<File>) {
        outputFilesGeneratedForPlugins.setValue(files)
    }

    fun removeOutputsGeneratedByPlugins() {
        val value = outputFilesGeneratedForPlugins.getAndRemoveValue()
        value.forEach {
            icContext.reporter.debug { "Deleting $it on clearing cache for plugin outputs" }
            icContext.transaction.deleteFile(it.toPath())
        }
    }
}

private class ListLikeMap(
    storageFile: File,
    fileDescriptor: KeyDescriptor<File>,
    icContext: IncrementalCompilationContext,
) : AbstractBasicMap<Int, List<File>>(
    storageFile,
    keyDescriptor = IntExternalizer.toDescriptor(),
    valueExternalizer = ListExternalizer(fileDescriptor),
    icContext,
) {
    fun getValue(): List<File> {
        return get(0).orEmpty()
    }

    fun getAndRemoveValue(): List<File> {
        return getValue().also { remove(0) }
    }

    fun setValue(value: List<File>) {
        set(0, value)
    }
}
