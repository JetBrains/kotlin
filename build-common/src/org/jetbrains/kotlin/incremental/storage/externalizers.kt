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

package org.jetbrains.kotlin.incremental.storage

import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.io.File
import java.util.*

object LookupSymbolKeyDescriptor : KeyDescriptor<LookupSymbolKey> {
    override fun read(input: DataInput): LookupSymbolKey {
        val first = input.readInt()
        val second = input.readInt()

        return LookupSymbolKey(first, second)
    }

    override fun save(output: DataOutput, value: LookupSymbolKey) {
        output.writeInt(value.nameHash)
        output.writeInt(value.scopeHash)
    }

    override fun getHashCode(value: LookupSymbolKey): Int = value.hashCode()

    override fun isEqual(val1: LookupSymbolKey, val2: LookupSymbolKey): Boolean = val1 == val2
}

object ProtoMapValueExternalizer : DataExternalizer<ProtoMapValue> {
    override fun save(output: DataOutput, value: ProtoMapValue) {
        output.writeBoolean(value.isPackageFacade)
        output.writeInt(value.bytes.size)
        output.write(value.bytes)
        output.writeInt(value.strings.size)

        for (string in value.strings) {
            output.writeUTF(string)
        }
    }

    override fun read(input: DataInput): ProtoMapValue {
        val isPackageFacade = input.readBoolean()
        val bytesLength = input.readInt()
        val bytes = ByteArray(bytesLength)
        input.readFully(bytes, 0, bytesLength)
        val stringsLength = input.readInt()
        val strings = Array<String>(stringsLength) { input.readUTF() }
        return ProtoMapValue(isPackageFacade, bytes, strings)
    }
}


abstract class StringMapExternalizer<T> : DataExternalizer<Map<String, T>> {
    override fun save(output: DataOutput, map: Map<String, T>?) {
        output.writeInt(map!!.size)

        for ((key, value) in map.entries) {
            IOUtil.writeString(key, output)
            writeValue(output, value)
        }
    }

    override fun read(input: DataInput): Map<String, T>? {
        val size = input.readInt()
        val map = HashMap<String, T>(size)

        repeat(size) {
            val name = IOUtil.readString(input)!!
            map[name] = readValue(input)
        }

        return map
    }

    protected abstract fun writeValue(output: DataOutput, value: T)
    protected abstract fun readValue(input: DataInput): T
}


object StringToLongMapExternalizer : StringMapExternalizer<Long>() {
    override fun readValue(input: DataInput): Long = input.readLong()

    override fun writeValue(output: DataOutput, value: Long) {
        output.writeLong(value)
    }
}

object ConstantsMapExternalizer : DataExternalizer<Map<String, Any>> {
    override fun save(output: DataOutput, map: Map<String, Any>?) {
        output.writeInt(map!!.size)
        for (name in map.keys.sorted()) {
            IOUtil.writeString(name, output)
            val value = map[name]!!
            when (value) {
                is Int -> {
                    output.writeByte(Kind.INT.ordinal)
                    output.writeInt(value)
                }
                is Float -> {
                    output.writeByte(Kind.FLOAT.ordinal)
                    output.writeFloat(value)
                }
                is Long -> {
                    output.writeByte(Kind.LONG.ordinal)
                    output.writeLong(value)
                }
                is Double -> {
                    output.writeByte(Kind.DOUBLE.ordinal)
                    output.writeDouble(value)
                }
                is String -> {
                    output.writeByte(Kind.STRING.ordinal)
                    IOUtil.writeString(value, output)
                }
                else -> throw IllegalStateException("Unexpected constant class: ${value::class.java}")
            }
        }
    }

    override fun read(input: DataInput): Map<String, Any>? {
        val size = input.readInt()
        val map = HashMap<String, Any>(size)

        repeat(size) {
            val name = IOUtil.readString(input)!!
            val kind = Kind.values()[input.readByte().toInt()]

            val value: Any = when (kind) {
                Kind.INT -> input.readInt()
                Kind.FLOAT -> input.readFloat()
                Kind.LONG -> input.readLong()
                Kind.DOUBLE -> input.readDouble()
                Kind.STRING -> IOUtil.readString(input)!!
            }

            map[name] = value
        }

        return map
    }

    private enum class Kind {
        INT, FLOAT, LONG, DOUBLE, STRING
    }
}

object IntExternalizer : DataExternalizer<Int> {
    override fun read(input: DataInput): Int = input.readInt()

    override fun save(output: DataOutput, value: Int) {
        output.writeInt(value)
    }
}

object PathStringDescriptor : EnumeratorStringDescriptor() {
    override fun getHashCode(value: String) = FileUtil.pathHashCode(value)

    override fun isEqual(val1: String, val2: String?) = FileUtil.pathsEqual(val1, val2)
}

object FileKeyDescriptor : KeyDescriptor<File> {
    override fun read(input: DataInput): File = File(input.readUTF())

    override fun save(output: DataOutput, value: File) {
        output.writeUTF(value.canonicalPath)
    }

    override fun getHashCode(value: File?): Int =
            FileUtil.FILE_HASHING_STRATEGY.computeHashCode(value)

    override fun isEqual(val1: File?, val2: File?): Boolean =
            FileUtil.FILE_HASHING_STRATEGY.equals(val1, val2)
}

open class CollectionExternalizer<T>(
        private val elementExternalizer: DataExternalizer<T>,
        private val newCollection: () -> MutableCollection<T>
) : DataExternalizer<Collection<T>> {
    override fun read(input: DataInput): Collection<T> {
        val result = newCollection()
        val stream = input as DataInputStream

        while (stream.available() > 0) {
            result.add(elementExternalizer.read(stream))
        }

        return result
    }

    override fun save(output: DataOutput, value: Collection<T>) {
        value.forEach { elementExternalizer.save(output, it) }
    }
}

object StringCollectionExternalizer : CollectionExternalizer<String>(EnumeratorStringDescriptor(), { HashSet() })

object IntCollectionExternalizer : CollectionExternalizer<Int>(IntExternalizer, { HashSet() })
