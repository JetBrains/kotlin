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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileWithId
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.util.io.DataInputOutputUtil.readSeq
import com.intellij.util.io.DataInputOutputUtil.writeSeq
import com.intellij.util.io.IOUtil.readUTF
import com.intellij.util.io.IOUtil.writeUTF
import java.io.DataInput
import java.io.DataOutput
import java.io.File
import kotlin.reflect.KProperty
import kotlin.script.experimental.dependencies.ScriptDependencies

var VirtualFile.scriptDependencies: ScriptDependencies? by ScriptDependenciesProperty()
private val scriptDependencies = FileAttribute("kotlin-script-dependencies", 3, false)

private class ScriptDependenciesProperty {

    operator fun setValue(file: VirtualFile, property: KProperty<*>, newValue: ScriptDependencies?) {
        if (file !is VirtualFileWithId) return

        if (newValue != null) {
            val output = scriptDependencies.writeAttribute(file)
            output.use {
                with(newValue) {
                    with(output) {
                        writeFileList(classpath)
                        writeStringList(imports)
                        writeNullable(javaHome, DataOutput::writeFile)
                        writeFileList(scripts)
                        writeFileList(sources)

                    }
                }
            }
        }
    }

    operator fun getValue(file: VirtualFile, property: KProperty<*>): ScriptDependencies? {
        if (file !is VirtualFileWithId) return null

        val input = scriptDependencies.readAttribute(file)
        return input?.use {
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