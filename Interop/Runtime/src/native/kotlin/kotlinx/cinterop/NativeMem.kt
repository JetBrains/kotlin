package kotlinx.cinterop

import konan.internal.Intrinsic

internal inline val pointerSize: Int
    get() = getPointerSize()

@Intrinsic external fun getPointerSize(): Int

// TODO: do not use singleton because it leads to init-check on any access.
object nativeMemUtils {
    @Intrinsic external fun getByte(mem: NativePointed): Byte
    @Intrinsic external fun putByte(mem: NativePointed, value: Byte)

    @Intrinsic external fun getShort(mem: NativePointed): Short
    @Intrinsic external fun putShort(mem: NativePointed, value: Short)

    @Intrinsic external fun getInt(mem: NativePointed): Int
    @Intrinsic external fun putInt(mem: NativePointed, value: Int)

    @Intrinsic external fun getLong(mem: NativePointed): Long
    @Intrinsic external fun putLong(mem: NativePointed, value: Long)

    @Intrinsic external fun getFloat(mem: NativePointed): Float
    @Intrinsic external fun putFloat(mem: NativePointed, value: Float)

    @Intrinsic external fun getDouble(mem: NativePointed): Double
    @Intrinsic external fun putDouble(mem: NativePointed, value: Double)

    @Intrinsic external fun getNativePtr(mem: NativePointed): NativePtr
    @Intrinsic external fun putNativePtr(mem: NativePointed, value: NativePtr)

    fun getByteArray(source: NativePointed, dest: ByteArray, length: Int) {
        val sourceArray: CArray<CInt8Var> = source.reinterpret()
        for (index in 0 .. length - 1) {
            dest[index] = sourceArray[index].value
        }
    }

    fun putByteArray(source: ByteArray, dest: NativePointed, length: Int) {
        val destArray: CArray<CInt8Var> = dest.reinterpret()
        for (index in 0 .. length - 1) {
            destArray[index].value = source[index]
        }
    }

    private class NativeAllocated(override val rawPtr: NativePtr) : NativePointed

    fun alloc(size: Long, align: Int): NativePointed {
        val ptr = malloc(size, align)
        if (ptr == nativeNullPtr) {
            throw OutOfMemoryError("unable to allocate native memory")
        }
        return NativeAllocated(ptr)
    }

    fun free(mem: NativePointed) {
        free(mem.rawPtr)
    }
}

@SymbolName("Kotlin_interop_malloc")
private external fun malloc(size: Long, align: Int): NativePtr

@SymbolName("Kotlin_interop_free")
private external fun free(ptr: NativePtr)