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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.*

class LookupSymbolKeyDescriptor(
    /** If `true`, original values are saved; if `false`, only hashes are saved. */
    private val storeFullFqNames: Boolean = false
) : KeyDescriptor<LookupSymbolKey> {

    override fun read(input: DataInput): LookupSymbolKey {
        // Note: The value of the storeFullFqNames variable below may or may not be the same as LookupSymbolKeyDescriptor.storeFullFqNames.
        // Byte value `0` means storeFullFqNames == true, see `save` function below.
        val storeFullFqNames = when (val byteValue = input.readByte().toInt()) {
            0 -> true
            1 -> false
            else -> error("Unexpected byte value for storeFullFqNames: $byteValue")
        }
        return if (storeFullFqNames) {
            val name = input.readUTF()
            val scope = input.readUTF()
            LookupSymbolKey(name.hashCode(), scope.hashCode(), name, scope)
        } else {
            val nameHash = input.readInt()
            val scopeHash = input.readInt()
            LookupSymbolKey(nameHash, scopeHash, "", "")
        }
    }

    override fun save(output: DataOutput, value: LookupSymbolKey) {
        // Write a Byte value `0` to represent storeFullFqNames == true for historical reasons (if we switch this value to `1` or write a
        // Boolean instead, it might impact some tests).
        output.writeByte(if (storeFullFqNames) 0 else 1)
        if (storeFullFqNames) {
            output.writeUTF(value.name)
            output.writeUTF(value.scope)
        } else {
            output.writeInt(value.nameHash)
            output.writeInt(value.scopeHash)
        }
    }

    override fun getHashCode(value: LookupSymbolKey): Int = value.hashCode()

    override fun isEqual(val1: LookupSymbolKey, val2: LookupSymbolKey): Boolean = val1 == val2
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

object JvmClassNameExternalizer : DataExternalizer<JvmClassName> {

    override fun save(output: DataOutput, jvmClassName: JvmClassName) {
        output.writeString(jvmClassName.internalName)
    }

    override fun read(input: DataInput): JvmClassName {
        return JvmClassName.byInternalName(input.readString())
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

    // The constants' values are provided by ASM, so their types can only be the following.
    // See https://asm.ow2.io/javadoc/org/objectweb/asm/ClassVisitor.html#visitField(int,java.lang.String,java.lang.String,java.lang.String,java.lang.Object)
    // (Note: Boolean constants have Integer (0, 1) values in ASM.)
    private enum class Kind {
        INT, FLOAT, LONG, DOUBLE, STRING
    }
}

fun <T> DataExternalizer<T>.saveToFile(file: File, value: T) {
    return DataOutputStream(FileOutputStream(file).buffered()).use {
        save(it, value)
    }
}

fun <T> DataExternalizer<T>.loadFromFile(file: File): T {
    return DataInputStream(FileInputStream(file).buffered()).use {
        read(it)
    }
}

fun <T> DataExternalizer<T>.toByteArray(value: T): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    DataOutputStream(byteArrayOutputStream.buffered()).use {
        save(it, value)
    }
    return byteArrayOutputStream.toByteArray()
}

fun <T> DataExternalizer<T>.fromByteArray(byteArray: ByteArray): T {
    return DataInputStream(ByteArrayInputStream(byteArray).buffered()).use {
        read(it)
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

abstract class GenericCollectionExternalizer<T, C : Collection<T>>(
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
        // We want `collection` to be both a mutable collection (so we can add elements to it as done above) and a type that can be safely
        // converted to type `C` (to be used as the returned value of this method). However, there is no type-safe way to express that, so
        // we have to use this unsafe cast.
        @Suppress("UNCHECKED_CAST")
        return collection as C
    }
}

class ListExternalizer<T>(elementExternalizer: DataExternalizer<T>) :
    GenericCollectionExternalizer<T, List<T>>(elementExternalizer, { size -> ArrayList(size) })

class SetExternalizer<T>(elementExternalizer: DataExternalizer<T>) :
    GenericCollectionExternalizer<T, Set<T>>(elementExternalizer, { size -> LinkedHashSet(size) })

open class MapExternalizer<K, V, M : Map<K, V>>(
    private val keyExternalizer: DataExternalizer<K>,
    private val valueExternalizer: DataExternalizer<V>,
    private val newMap: (size: Int) -> MutableMap<K, V> = { size -> LinkedHashMap(size) }
) : DataExternalizer<M> {

    override fun save(output: DataOutput, map: M) {
        output.writeInt(map.size)
        for ((key, value) in map) {
            keyExternalizer.save(output, key)
            valueExternalizer.save(output, value)
        }
    }

    override fun read(input: DataInput): M {
        val size = input.readInt()
        val map = newMap(size)
        repeat(size) {
            val key = keyExternalizer.read(input)
            val value = valueExternalizer.read(input)
            map[key] = value
        }
        @Suppress("UNCHECKED_CAST")
        return map as M
    }
}

class LinkedHashMapExternalizer<K, V>(
    keyExternalizer: DataExternalizer<K>,
    valueExternalizer: DataExternalizer<V>
) : MapExternalizer<K, V, LinkedHashMap<K, V>>(keyExternalizer, valueExternalizer, { size -> LinkedHashMap(size) })
