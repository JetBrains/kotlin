package kotlin_native.interop

import sun.misc.Unsafe

data class NativePtr private constructor(internal val value: Long) {

    fun displacedBy(offset: Int) = NativePtr(value + offset)

    companion object {
        fun byValue(value: Long): NativePtr? {
            return if (value == 0L) {
                null
            } else {
                NativePtr(value)
            }
        }
    }
}

fun NativePtr?.asLong() = if (this == null) 0L else value

internal object bridge {

    fun malloc(size: Int): NativePtr = NativePtr.byValue(unsafe.allocateMemory(size.toLong()))!!
    fun free(ptr: NativePtr) = unsafe.freeMemory(ptr.asLong())

    fun getInt64(ptr: NativePtr): Long = unsafe.getLong(ptr.asLong())
    fun putInt64(ptr: NativePtr, value: Long) = unsafe.putLong(ptr.asLong(), value)

    fun getPtr(ptr: NativePtr): NativePtr? = NativePtr.byValue(unsafe.getLong(ptr.asLong()))
    fun putPtr(ptr: NativePtr, value: NativePtr?) = unsafe.putLong(ptr.asLong(), value.asLong())

    fun getInt32(ptr: NativePtr): Int = unsafe.getInt(ptr.asLong())
    fun putInt32(ptr: NativePtr, value: Int) = unsafe.putInt(ptr.asLong(), value)

    fun getInt16(ptr: NativePtr): Short = unsafe.getShort(ptr.asLong())
    fun putInt16(ptr: NativePtr, value: Short) = unsafe.putShort(ptr.asLong(), value)

    fun getInt8(ptr: NativePtr): Byte = unsafe.getByte(ptr.asLong())
    fun putInt8(ptr: NativePtr, value: Byte) = unsafe.putByte(ptr.asLong(), value)

    private val unsafe = with(Unsafe::class.java.getDeclaredField("theUnsafe")) {
        isAccessible = true
        return@with this.get(null) as Unsafe
    }
}