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
import com.intellij.util.io.DataInputOutputUtil.readSeq
import com.intellij.util.io.DataInputOutputUtil.writeSeq
import com.intellij.util.io.IOUtil.readUTF
import com.intellij.util.io.IOUtil.writeUTF
import org.jetbrains.kotlin.idea.caches.FileAttributeService
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import kotlin.script.dependencies.ScriptDependencies

// TODO_R: write nullable, write version
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
                output.writeFileList(classpath)
                output.writeStringList(imports)
                // TODO: actually read/write nullable
                output.writeFile(javaHome ?: File(""))
                output.writeFileList(scripts)
                output.writeFileList(sources)
            }
        }
    }

    fun read(virtualFile: VirtualFile): ScriptDependencies? {
        if (virtualFile !is VirtualFileWithId) return null
        return fileAttributeService.read(virtualFile, ID) { input ->
            ScriptDependencies(
                    classpath = input.readFileList(),
                    imports = input.readStringList(),
                    javaHome = input.readFile(),
                    scripts = input.readFileList(),
                    sources = input.readFileList()
            )
        }?.value
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
