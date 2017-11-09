/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.FileAttribute
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.DataInputOutputUtil
import java.io.DataInput
import java.io.DataOutput

class FileAttributeServiceImpl : FileAttributeService {
    val attributes: MutableMap<String, FileAttribute> = ContainerUtil.newConcurrentMap()

    override fun register(id: String, version: Int, fixedSize: Boolean) {
        attributes[id] = FileAttribute(id, version, fixedSize)
    }

    override fun <T: Enum<T>> writeEnumAttribute(id: String, file: VirtualFile, value: T): CachedAttributeData<T> {
        return write(file, id, value) { output, v ->
            DataInputOutputUtil.writeINT(output, v.ordinal)
        }
    }

    override fun <T: Enum<T>> readEnumAttribute(id: String, file: VirtualFile, klass: Class<T>): CachedAttributeData<T>? {
        return read(file, id) { input ->
            deserializeEnumValue(DataInputOutputUtil.readINT(input), klass)
        }
    }


    override fun writeBooleanAttribute(id: String, file: VirtualFile, value: Boolean): CachedAttributeData<Boolean> {
        return write(file, id, value) { output, v ->
            DataInputOutputUtil.writeINT(output, if (v) 1 else 0)
        }
    }

    override fun readBooleanAttribute(id: String, file: VirtualFile): CachedAttributeData<Boolean>? {
        return read(file, id) { input ->
            DataInputOutputUtil.readINT(input) > 0
        }
    }

    override fun <T> write(file: VirtualFile, id: String, value: T, writeValueFun: (DataOutput, T) -> Unit): CachedAttributeData<T> {
        val attribute = attributes[id] ?: throw IllegalArgumentException("Attribute with $id wasn't registered")

        val data = CachedAttributeData(value, timeStamp = file.timeStamp)

        attribute.writeAttribute(file).use {
            DataInputOutputUtil.writeTIME(it, data.timeStamp)
            writeValueFun(it, value)
        }

        return data
    }

    override fun <T> read(file: VirtualFile, id: String, readValueFun: (DataInput) -> T): CachedAttributeData<T>? {
        val attribute = attributes[id] ?: throw IllegalArgumentException("Attribute with $id wasn't registered")

        val stream = attribute.readAttribute(file) ?: return null
        return stream.use {
            val timeStamp = DataInputOutputUtil.readTIME(it)
            val value = readValueFun(it)

            if (file.timeStamp == timeStamp) {
                CachedAttributeData(value, timeStamp)
            }
            else {
                null
            }
        }
    }

    private fun <T: Enum<T>> deserializeEnumValue(i: Int, klass: Class<T>): T {
        val method = klass.getMethod("values")

        @Suppress("UNCHECKED_CAST")
        val values = method.invoke(null) as Array<T>

        return values[i]
    }
}

