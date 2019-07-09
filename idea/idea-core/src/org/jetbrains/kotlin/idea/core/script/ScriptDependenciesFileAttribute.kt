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
import org.jetbrains.kotlin.idea.core.util.*
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper
import java.io.*
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.dependencies.ScriptDependencies

var VirtualFile.scriptDependencies: ScriptDependencies? by cachedFileAttribute(
    name = "kotlin-script-dependencies",
    version = 3,
    read = {
        ScriptDependencies(
            classpath = readFileList(),
            imports = readStringList(),
            javaHome = readNullable(DataInput::readFile),
            scripts = readFileList(),
            sources = readFileList()
        )
    },
    write = {
        with(it) {
            writeFileList(classpath)
            writeStringList(imports)
            writeNullable(javaHome, DataOutput::writeFile)
            writeFileList(scripts)
            writeFileList(sources)
        }
    }
)

var VirtualFile.scriptCompilationConfiguration: ScriptCompilationConfiguration? by cachedFileAttribute(
    name = "kotlin-script-compilation-configuration",
    version = 1,
    read = {
        val size = readInt()
        val bytes = ByteArray(size)
        read(bytes, 0, size)
        val bis = ByteArrayInputStream(bytes)
        ObjectInputStream(bis).use { ois ->
            ois.readObject() as ScriptCompilationConfiguration
        }
    },
    write = {
        val os = ByteArrayOutputStream()
        ObjectOutputStream(os).use { oos ->
            oos.writeObject((it as? ScriptCompilationConfigurationWrapper.FromCompilationConfiguration)?.configuration)
        }
        val bytes = os.toByteArray()
        writeInt(bytes.size)
        write(bytes)
    }
)
