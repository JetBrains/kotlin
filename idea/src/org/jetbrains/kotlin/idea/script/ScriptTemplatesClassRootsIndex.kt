/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.*
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.scripting.definitions.SCRIPT_DEFINITION_MARKERS_EXTENSION_WITH_DOT
import org.jetbrains.kotlin.scripting.definitions.SCRIPT_DEFINITION_MARKERS_PATH
import java.io.DataInput
import java.io.DataOutput

object UnitKey : KeyDescriptor<Unit> {
    override fun getHashCode(value: Unit) = 0
    override fun isEqual(val1: Unit, val2: Unit) = true
    override fun save(out: DataOutput, value: Unit?) = Unit
    override fun read(`in`: DataInput) = Unit
}

abstract class FileListIndex :
    ScalarIndexExtension<Unit>(),
    DataIndexer<Unit, Void, FileContent> {

    override fun getIndexer() = this
    override fun getKeyDescriptor() = UnitKey
    override fun dependsOnFileContent() = false
    override fun map(inputData: FileContent) = mapOf(Unit to null)
}

class ScriptTemplatesClassRootsIndex :
    FileListIndex(),
    FileBasedIndex.InputFilter {

    companion object {
        val NAME = ID.create<Unit, Void>(ScriptTemplatesClassRootsIndex::class.java.canonicalName)
        const val VALUE = "MY_VALUE"
        private val suffix = SCRIPT_DEFINITION_MARKERS_PATH.removeSuffix("/")
    }

    override fun getName() = NAME
    override fun getVersion(): Int = 3
    override fun indexDirectories(): Boolean = false
    override fun getInputFilter(): FileBasedIndex.InputFilter = this

    override fun acceptInput(file: VirtualFile): Boolean {
        val parent = file.parent ?: return false
        return parent.isDirectory
                && parent.path.endsWith(suffix)
                && file.path.endsWith(SCRIPT_DEFINITION_MARKERS_EXTENSION_WITH_DOT)
    }
}