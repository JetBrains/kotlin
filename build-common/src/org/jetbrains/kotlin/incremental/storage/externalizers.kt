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

import com.intellij.util.containers.hash.EqualityPolicy
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.IOUtil
import com.intellij.util.io.KeyDescriptor
import org.jetbrains.kotlin.inline.InlineFunction
import org.jetbrains.kotlin.inline.InlineFunctionOrAccessor
import org.jetbrains.kotlin.inline.InlinePropertyAccessor
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.*

class DefaultEqualityPolicy<T> : EqualityPolicy<T> {
    override fun getHashCode(value: T): Int = value.hashCode()
    override fun isEqual(value1: T, value2: T): Boolean = (value1 == value2)
}

fun <T> DataExternalizer<T>.toDescriptor(): KeyDescriptor<T> =
    object : KeyDescriptor<T>,
        DataExternalizer<T> by this,
        EqualityPolicy<T> by DefaultEqualityPolicy<T>() {
    }

class LookupSymbolKeyDescriptor(
    /** If `true`, original values are saved; if `false`, only hashes are saved. */
    private val storeFullFqNames: Boolean = false
) : KeyDescriptor<LookupSymbolKey>, EqualityPolicy<LookupSymbolKey> by DefaultEqualityPolicy() {

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
            packageFqName = FqNameExternalizer.read(input),
            relativeClassName = FqNameExternalizer.read(input),
            isLocal = input.readBoolean()
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

object FloatExternalizer : DataExternalizer<Float> {
    override fun save(output: DataOutput, value: Float) = output.writeFloat(value)
    override fun read(input: DataInput): Float = input.readFloat()
}

object DoubleExternalizer : DataExternalizer<Double> {
    override fun save(output: DataOutput, value: Double) = output.writeDouble(value)
    override fun read(input: DataInput): Double = input.readDouble()
}

object StringExternalizer : DataExternalizer<String> {
    override fun save(output: DataOutput, value: String) = IOUtil.writeString(value, output)
    override fun read(input: DataInput): String = IOUtil.readString(input)
}

/** [DataExternalizer] that delegates to another [DataExternalizer] depending on the type of the object to externalize. */
class DelegateDataExternalizer<T>(
    val types: List<Class<out T>>,
    val typesExternalizers: List<DataExternalizer<out T>>
) : DataExternalizer<T> {

    init {
        check(types.size == typesExternalizers.size)
        check(types.size < Byte.MAX_VALUE) // We will writeByte(index), so we need lastIndex (types.size - 1) <= Byte.MAX_VALUE
    }

    override fun save(output: DataOutput, objectToExternalize: T) {
        val type = types.single { it.isAssignableFrom(objectToExternalize!!::class.java) }
        val typeIndex = types.indexOf(type)

        output.writeByte(typeIndex)
        @Suppress("UNCHECKED_CAST")
        (typesExternalizers[typeIndex] as DataExternalizer<T>).save(output, objectToExternalize)
    }

    override fun read(input: DataInput): T {
        val typeIndex = input.readByte().toInt()
        return typesExternalizers[typeIndex].read(input)
    }
}

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

/**
 * DEPRECATED: [DataExternalizer] for a [Collection], whose implementation is tied to [com.intellij.util.io.PersistentHashMap] (e.g., the
 * [read] method reads until the stream ends -- this can only work with a [com.intellij.util.io.PersistentHashMap]).
 *
 * Use [CollectionExternalizerV2] if possible.
 */
private class CollectionExternalizerForPersistentHashMap<T>(
    private val elementExternalizer: DataExternalizer<T>,
    private val newCollection: () -> MutableCollection<T>,
) : DataExternalizer<Collection<T>> {

    override fun save(output: DataOutput, value: Collection<T>) {
        value.forEach { elementExternalizer.save(output, it) }
    }

    override fun read(input: DataInput): Collection<T> {
        val result = newCollection()
        val stream = input as DataInputStream

        while (stream.available() > 0) {
            result.add(elementExternalizer.read(stream))
        }

        return result
    }
}

/**
 * DEPRECATED: This class should not be used because its implementation is tied to [com.intellij.util.io.PersistentHashMap]
 * (see [CollectionExternalizerForPersistentHashMap]).
 *
 * Currently, we can't change the name or implementation of this class because it is still used by the `compiler-reference-index` module in
 * the Kotlin IDEA plugin and that code relies on this name and implementation being unchanged (see KTIJ-27258).
 *
 * Once we remove that dependency, we can remove this class.
 */
class CollectionExternalizer<T>(
    private val elementExternalizer: DataExternalizer<T>,
    private val newCollection: () -> MutableCollection<T>,
) : DataExternalizer<Collection<T>> by CollectionExternalizerForPersistentHashMap(elementExternalizer, newCollection)

/** DEPRECATED: See [CollectionExternalizer]. */
@Suppress("unused") // See `CollectionExternalizer`
object IntCollectionExternalizer :
    DataExternalizer<Collection<Int>> by CollectionExternalizerForPersistentHashMap(IntExternalizer, { ArrayList() })

/** [DataExternalizer] for a [Collection]. */
open class CollectionExternalizerV2<T, C : Collection<T>>(
    private val elementExternalizer: DataExternalizer<T>,
    private val newCollection: (size: Int) -> MutableCollection<T> = { size -> ArrayList(size) },
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
    CollectionExternalizerV2<T, List<T>>(elementExternalizer, { size -> ArrayList(size) })

class SetExternalizer<T>(elementExternalizer: DataExternalizer<T>) :
    CollectionExternalizerV2<T, Set<T>>(elementExternalizer, { size -> LinkedHashSet(size) })

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

object JvmMethodSignatureExternalizer : DataExternalizer<JvmMemberSignature.Method> {

    override fun save(output: DataOutput, method: JvmMemberSignature.Method) {
        StringExternalizer.save(output, method.name)
        StringExternalizer.save(output, method.desc)
    }

    override fun read(input: DataInput): JvmMemberSignature.Method {
        return JvmMemberSignature.Method(
            name = StringExternalizer.read(input),
            desc = StringExternalizer.read(input)
        )
    }
}

object InlineFunctionOrAccessorExternalizer : DataExternalizer<InlineFunctionOrAccessor> by DelegateDataExternalizer(
    types = listOf(InlineFunction::class.java, InlinePropertyAccessor::class.java),
    typesExternalizers = listOf(InlineFunctionExternalizer, InlinePropertyAccessorExternalizer)
)

private object InlineFunctionExternalizer : DataExternalizer<InlineFunction> {

    override fun save(output: DataOutput, function: InlineFunction) {
        JvmMethodSignatureExternalizer.save(output, function.jvmMethodSignature)
        StringExternalizer.save(output, function.kotlinFunctionName)
    }

    override fun read(input: DataInput): InlineFunction {
        return InlineFunction(
            jvmMethodSignature = JvmMethodSignatureExternalizer.read(input),
            kotlinFunctionName = StringExternalizer.read(input)
        )
    }
}

private object InlinePropertyAccessorExternalizer : DataExternalizer<InlinePropertyAccessor> {

    override fun save(output: DataOutput, accessor: InlinePropertyAccessor) {
        JvmMethodSignatureExternalizer.save(output, accessor.jvmMethodSignature)
        StringExternalizer.save(output, accessor.propertyName)
    }

    override fun read(input: DataInput): InlinePropertyAccessor {
        return InlinePropertyAccessor(
            jvmMethodSignature = JvmMethodSignatureExternalizer.read(input),
            propertyName = StringExternalizer.read(input)
        )
    }
}
