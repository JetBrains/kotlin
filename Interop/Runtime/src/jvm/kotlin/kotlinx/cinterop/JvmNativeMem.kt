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

package kotlinx.cinterop

import sun.misc.Unsafe

private val NativePointed.address: Long
    get() = this.rawPtr

private enum class DataModel(val pointerSize: Long) {
    _32BIT(4),
    _64BIT(8)
}

private val dataModel: DataModel = when (System.getProperty("sun.arch.data.model")) {
    null -> TODO()
    "32" -> DataModel._32BIT
    "64" -> DataModel._64BIT
    else -> throw IllegalStateException()
}

val pointerSize: Int = dataModel.pointerSize.toInt()

object nativeMemUtils {
    fun getByte(mem: NativePointed) = unsafe.getByte(mem.address)
    fun putByte(mem: NativePointed, value: Byte) = unsafe.putByte(mem.address, value)

    fun getShort(mem: NativePointed) = unsafe.getShort(mem.address)
    fun putShort(mem: NativePointed, value: Short) = unsafe.putShort(mem.address, value)
    
    fun getInt(mem: NativePointed) = unsafe.getInt(mem.address)
    fun putInt(mem: NativePointed, value: Int) = unsafe.putInt(mem.address, value)
    
    fun getLong(mem: NativePointed) = unsafe.getLong(mem.address)
    fun putLong(mem: NativePointed, value: Long) = unsafe.putLong(mem.address, value)

    fun getFloat(mem: NativePointed) = unsafe.getFloat(mem.address)
    fun putFloat(mem: NativePointed, value: Float) = unsafe.putFloat(mem.address, value)

    fun getDouble(mem: NativePointed) = unsafe.getDouble(mem.address)
    fun putDouble(mem: NativePointed, value: Double) = unsafe.putDouble(mem.address, value)

    fun getNativePtr(mem: NativePointed): NativePtr = when (dataModel) {
        DataModel._32BIT -> getInt(mem).toLong()
        DataModel._64BIT -> getLong(mem)
    }

    fun putNativePtr(mem: NativePointed, value: NativePtr) = when (dataModel) {
        DataModel._32BIT -> putInt(mem, value.toInt())
        DataModel._64BIT -> putLong(mem, value)
    }

    fun getByteArray(source: NativePointed, dest: ByteArray, length: Int) {
        val clazz = ByteArray::class.java
        val baseOffset = unsafe.arrayBaseOffset(clazz).toLong();
        unsafe.copyMemory(null, source.address, dest, baseOffset, length.toLong())
    }

    fun putByteArray(source: ByteArray, dest: NativePointed, length: Int) {
        val clazz = ByteArray::class.java
        val baseOffset = unsafe.arrayBaseOffset(clazz).toLong();
        unsafe.copyMemory(source, baseOffset, null, dest.address, length.toLong())
    }

    fun getCharArray(source: NativePointed, dest: CharArray, length: Int) {
        val clazz = CharArray::class.java
        val baseOffset = unsafe.arrayBaseOffset(clazz).toLong();
        unsafe.copyMemory(null, source.address, dest, baseOffset, length.toLong() * 2)
    }

    fun putCharArray(source: CharArray, dest: NativePointed, length: Int) {
        val clazz = CharArray::class.java
        val baseOffset = unsafe.arrayBaseOffset(clazz).toLong();
        unsafe.copyMemory(source, baseOffset, null, dest.address, length.toLong() * 2)
    }

    fun zeroMemory(dest: NativePointed, length: Int): Unit = unsafe.setMemory(dest.address, length.toLong(), 0)

    internal class NativeAllocated(override val rawPtr: NativePtr) : NativePointed

    fun alloc(size: Long, align: Int): NativePointed {
        val address = unsafe.allocateMemory(
                if (size == 0L) 1L else size // It is a hack: `sun.misc.Unsafe` can't allocate zero bytes
        )

        if (address % align != 0L) TODO(align.toString())
        return interpretPointed<NativeAllocated>(address)
    }

    fun free(mem: NativePtr) {
        unsafe.freeMemory(mem)
    }

    private val unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
        isAccessible = true
        return@with this.get(null) as Unsafe
    }
}
