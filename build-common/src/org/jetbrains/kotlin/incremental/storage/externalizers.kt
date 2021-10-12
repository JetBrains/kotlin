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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutput
import java.util.*

/**
 * Storage versioning:
 * 0 - only name and value hashes are saved
 * 1 - name and scope are saved
 */
object LookupSymbolKeyDescriptor : KeyDescriptor<LookupSymbolKey> {
    override fun read(input: DataInput): LookupSymbolKey {
        val version = input.readByte()
        return when (version.toInt()) {
            0 -> {
                val name = input.readUTF()
                val scope = input.readUTF()
                LookupSymbolKey(name.hashCode(), scope.hashCode(), name, scope)
            }
            1 -> {
                val first = input.readInt()
                val second = input.readInt()
                LookupSymbolKey(first, second, "", "")
            }
            else -> throw RuntimeException("Unknown version of LookupSymbolKeyDescriptor=${version}")
        }
    }

    private val storeFullFqName = CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_CLASSPATH_SNAPSHOTS.value.toBooleanLenient() ?: false

    override fun save(output: DataOutput, value: LookupSymbolKey) {
        if (storeFullFqName) {
            output.writeByte(0)
            output.writeUTF(value.name)
            output.writeUTF(value.scope)
        } else {
            output.writeByte(1)
            output.writeInt(value.nameHash)
            output.writeInt(value.scopeHash)
        }
    }

    override fun getHashCode(value: LookupSymbolKey): Int = value.hashCode()

    override fun isEqual(val1: LookupSymbolKey, val2: LookupSymbolKey): Boolean = val1 == val2
}

object LookupSymbolExternalizer : DataExternalizer<LookupSymbol> {

    override fun save(output: DataOutput, lookupSymbol: LookupSymbol) {
        output.writeString(lookupSymbol.name)
        output.writeString(lookupSymbol.scope)
    }

    override fun read(input: DataInput): LookupSymbol {
        return LookupSymbol(name = input.readString(), scope = input.readString())
    }
}

object FqNameExternalizer : DataExternalizer<FqName> {

    override fun save(output: DataOutput, fqName: FqName) {
        output.writeString(fqName.asString())
    }

    override fun read(input: DataInput): FqName {
        return FqName(input.readString())
    }
}

object ClassIdExternalizer : DataExternalizer<ClassId> {

    override fun save(output: DataOutput, classId: ClassId) {
        FqNameExternalizer.save(output, classId.packageFqName)
        FqNameExternalizer.save(output, classId.relativeClassName)
        output.writeBoolean(classId.isLocal)
    }

    override fun read(input: DataInput): ClassId {
        return ClassId(
            /* packageFqName */ FqNameExternalizer.read(input),
            /* relativeClassName */ FqNameExternalizer.read(input),
            /* isLocal */ input.readBoolean()
        )
    }
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
            output.writeString(key)
            writeValue(output, value)
        }
    }

    override fun read(input: DataInput): Map<String, T>? {
        val size = input.readInt()
        val map = HashMap<String, T>(size)

        repeat(size) {
            val name = input.readString()
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

/** [DataExternalizer] for a Kotlin constant. */
object ConstantExternalizer : DataExternalizer<Any> {

    override fun save(output: DataOutput, value: Any) {
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
                output.writeString(value)
            }
            else -> throw IllegalStateException("Unexpected constant class: ${value::class.java}")
        }
    }

    override fun read(input: DataInput): Any {
        return when (Kind.values()[input.readByte().toInt()]) {
            Kind.INT -> input.readInt()
            Kind.FLOAT -> input.readFloat()
            Kind.LONG -> input.readLong()
            Kind.DOUBLE -> input.readDouble()
            Kind.STRING -> input.readString()
        }
    }

    private enum class Kind {
        INT, FLOAT, LONG, DOUBLE, STRING
    }
}

object IntExternalizer : DataExternalizer<Int> {
    override fun save(output: DataOutput, value: Int) = output.writeInt(value)
    override fun read(input: DataInput): Int = input.readInt()
}

object LongExternalizer : DataExternalizer<Long> {
    override fun save(output: DataOutput, value: Long) = output.writeLong(value)
    override fun read(input: DataInput): Long = input.readLong()
}

object StringExternalizer : DataExternalizer<String> {
    override fun save(output: DataOutput, value: String) = IOUtil.writeString(value, output)
    override fun read(input: DataInput): String = IOUtil.readString(input)
}

// Should be consistent with org.jetbrains.jps.incremental.storage.PathStringDescriptor for correct work of portable caches
object PathStringDescriptor : EnumeratorStringDescriptor() {
    private const val PORTABLE_CACHES_PROPERTY = "org.jetbrains.jps.portable.caches"
    private val PORTABLE_CACHES = java.lang.Boolean.getBoolean(PORTABLE_CACHES_PROPERTY)

    override fun getHashCode(path: String): Int {
        if (!PORTABLE_CACHES) return FileUtil.pathHashCode(path)
        // On case insensitive OS hash calculated from value converted to lower case
        return if (StringUtil.isEmpty(path)) 0 else FileUtil.toCanonicalPath(path).hashCode()
    }

    override fun isEqual(val1: String, val2: String?): Boolean {
        if (!PORTABLE_CACHES) return FileUtil.pathsEqual(val1, val2)
        // On case insensitive OS hash calculated from path converted to lower case
        if (val1 == val2) return true
        if (val2 == null) return false

        val path1 = FileUtil.toCanonicalPath(val1)
        val path2 = FileUtil.toCanonicalPath(val2)
        return path1 == path2
    }
}

/**
 * [DataExternalizer] for a [Collection].
 *
 * If you need a [DataExternalizer] for a more specific instance of [Collection] (e.g., [List]), use [ListExternalizer] or create another
 * instance of [GenericCollectionExternalizer].
 *
 * Note: The implementations of this class and [GenericCollectionExternalizer] are similar but not exactly the same: the latter reads and
 * writes the size of the collection to avoid resizing the collection when reading. Therefore, if we make this class extend
 * [GenericCollectionExternalizer] to share code, we will need to update some expected files in tests as the serialized data will change
 * slightly.
 */
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

fun DataOutput.writeString(value: String) = StringExternalizer.save(this, value)

fun DataInput.readString(): String = StringExternalizer.read(this)

class NullableValueExternalizer<T>(private val valueExternalizer: DataExternalizer<T>) : DataExternalizer<T> {

    override fun save(output: DataOutput, value: T?) {
        output.writeBoolean(value != null)
        value?.let {
            valueExternalizer.save(output, it)
        }
    }

    override fun read(input: DataInput): T? {
        return if (input.readBoolean()) {
            valueExternalizer.read(input)
        } else null
    }
}

object ByteArrayExternalizer : DataExternalizer<ByteArray> {

    override fun save(output: DataOutput, bytes: ByteArray) {
        output.writeInt(bytes.size)
        output.write(bytes)
    }

    override fun read(input: DataInput): ByteArray {
        val size = input.readInt()
        return ByteArray(size).also {
            input.readFully(it, 0, size)
        }
    }
}

open class GenericCollectionExternalizer<T, C : Collection<T>>(
    private val elementExternalizer: DataExternalizer<T>,
    private val newCollection: (size: Int) -> MutableCollection<T>
) : DataExternalizer<C> {

    override fun save(output: DataOutput, collection: C) {
        output.writeInt(collection.size)
        collection.forEach {
            elementExternalizer.save(output, it)
        }
    }

    override fun read(input: DataInput): C {
        val size = input.readInt()
        val collection = newCollection(size)
        repeat(size) {
            collection.add(elementExternalizer.read(input))
        }
        @Suppress("UNCHECKED_CAST")
        return collection as C
    }
}

class ListExternalizer<T>(elementExternalizer: DataExternalizer<T>) :
    GenericCollectionExternalizer<T, List<T>>(elementExternalizer, { size -> ArrayList(size) })

class SetExternalizer<T>(elementExternalizer: DataExternalizer<T>) :
    GenericCollectionExternalizer<T, Set<T>>(elementExternalizer, { size -> LinkedHashSet(size) })

class LinkedHashMapExternalizer<K, V>(
    private val keyExternalizer: DataExternalizer<K>,
    private val valueExternalizer: DataExternalizer<V>
) : DataExternalizer<LinkedHashMap<K, V>> {

    override fun save(output: DataOutput, map: LinkedHashMap<K, V>) {
        output.writeInt(map.size)
        for ((key, value) in map) {
            keyExternalizer.save(output, key)
            valueExternalizer.save(output, value)
        }
    }

    override fun read(input: DataInput): LinkedHashMap<K, V> {
        val size = input.readInt()
        val map = LinkedHashMap<K, V>(size)
        repeat(size) {
            val key = keyExternalizer.read(input)
            val value = valueExternalizer.read(input)
            map[key] = value
        }
        return map
    }
}
