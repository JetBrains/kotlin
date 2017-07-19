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

package org.jetbrains.kotlin.idea.core.script

import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.util.io.DataInputOutputUtil.*
import com.intellij.util.io.IOUtil.readUTF
import com.intellij.util.io.IOUtil.writeUTF
import org.jetbrains.kotlin.idea.caches.FileAttributeService
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import java.io.IOException
import kotlin.script.dependencies.ScriptDependencies

object ScriptDependenciesFileAttribute {
    private val VERSION = 1
    private val ID = "kotlin-script-dependencies"

    private val fileAttributeService = service<FileAttributeService>()

    init {
        fileAttributeService.register(ID, VERSION, false)
    }

    fun write(virtualFile: VirtualFile, dependencies: ScriptDependencies) {
        if (virtualFile !is VirtualFileWithId) return
        fileAttributeService.write(virtualFile, ID, dependencies) { output, dep ->
            with(dep) {
                output.writeInt(VERSION)

                writeDependencies(this, output)
            }
        }
    }

    fun read(virtualFile: VirtualFile): ScriptDependencies? {
        if (virtualFile !is VirtualFileWithId) return null

        return fileAttributeService.read(virtualFile, ID) { input ->
            val version = input.readInt()
            if (version != VERSION) null
            else readDependencies(input)
        }?.value
    }

    private fun writeDependencies(scriptDependencies: ScriptDependencies, output: DataOutput) {
        with(scriptDependencies) {
            with(output) {
                writeFileList(classpath)
                writeStringList(imports)
                writeNullable(javaHome, DataOutput::writeFile)
                writeFileList(scripts)
                writeFileList(sources)

            }
        }
    }

    private fun readDependencies(input: DataInput): ScriptDependencies {
        with(input) {
            return ScriptDependencies(
                    classpath = readFileList(),
                    imports = readStringList(),
                    javaHome = readNullable(DataInput::readFile),
                    scripts = readFileList(),
                    sources = readFileList()
            )
        }
    }
}

private fun DataInput.readStringList(): List<String> = readSeq(this) { readString() }
private fun DataInput.readFileList() = readSeq(this) { readFile() }
private fun DataInput.readString() = readUTF(this)
private fun DataInput.readFile() = readUTF(this).let { File(it) }

private fun DataOutput.writeFileList(iterable: Iterable<File>) = writeSeq(this, iterable.toList()) { writeFile(it) }
private fun DataOutput.writeFile(it: File) = writeString(it.canonicalPath)
private fun DataOutput.writeString(string: String) = writeUTF(this, string)
private fun DataOutput.writeStringList(iterable: Iterable<String>) = writeSeq(this, iterable.toList()) { writeString(it) }

private fun <T : Any> DataOutput.writeNullable(nullable: T?, writeT: DataOutput.(T) -> Unit) {
    writeBoolean(nullable != null)
    nullable?.let { writeT(it) }
}

private fun <T : Any> DataInput.readNullable(readT: DataInput.() -> T): T? {
    val hasValue = readBoolean()
    return if (hasValue) readT() else null
}

@Throws(IOException::class)
private fun <T> readSeq(`in`: DataInput, readElement: () -> T): List<T> {
    val size = readINT(`in`)
    val result = ArrayList<T>(size)
    for (i in 0 until size) {
        result.add(readElement())
    }
    return result
}

@Throws(IOException::class)
private fun <T> writeSeq(out: DataOutput, collection: Collection<T>, writeElement: (T) -> Unit) {
    writeINT(out, collection.size)
    for (t in collection) {
        writeElement(t)
    }
}