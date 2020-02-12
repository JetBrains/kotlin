/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.*
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.scripting.definitions.SCRIPT_DEFINITION_MARKERS_PATH
import java.io.DataInput
import java.io.DataOutput
import java.util.*

class ScriptTemplatesClassRootsIndex :
    ScalarIndexExtension<String>(),
    FileBasedIndex.InputFilter, KeyDescriptor<String>,
    DataIndexer<String, Void, FileContent> {

    companion object {
        val KEY = ID.create<String, Void>(ScriptTemplatesClassRootsIndex::class.java.canonicalName)

        private val suffix = SCRIPT_DEFINITION_MARKERS_PATH.removeSuffix("/")
    }

    override fun getName(): ID<String, Void> = KEY

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = this

    override fun getKeyDescriptor(): KeyDescriptor<String> = this

    override fun getInputFilter(): FileBasedIndex.InputFilter = this

    override fun dependsOnFileContent() = false

    override fun getVersion(): Int = 1

    override fun indexDirectories(): Boolean = true

    override fun acceptInput(file: VirtualFile): Boolean {
        return file.isDirectory && file.path.endsWith(suffix)
    }

    override fun save(out: DataOutput, value: String) {
        IOUtil.writeUTF(out, value)
    }

    override fun read(input: DataInput): String? {
        return IOUtil.readUTF(input)
    }

    override fun getHashCode(value: String): Int {
        return value.hashCode()
    }

    override fun isEqual(val1: String?, val2: String?): Boolean {
        return val1 == val2
    }

    override fun map(inputData: FileContent): Map<String?, Void?> {
        return Collections.singletonMap(inputData.file.url, null)
    }
}