// TARGET_BACKEND: WASM

@file:OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)

import kotlin.wasm.unsafe.*

external class View : JsAny {
    fun getInt8(address: UInt, littleEndian: Boolean): Byte
    fun getInt16(address: UInt, littleEndian: Boolean): Short
    fun getInt32(address: UInt, littleEndian: Boolean): Int
    fun getBigInt64(address: UInt, littleEndian: Boolean): Long
    fun setInt8(address: UInt, value: Byte, littleEndian: Boolean): Unit
    fun setInt16(address: UInt, value: Short, littleEndian: Boolean): Unit
    fun setInt32(address: UInt, value: Int, littleEndian: Boolean): Unit
    fun setBigInt64(address: UInt, value: Long, littleEndian: Boolean): Unit
}

fun getDataViewFromMemory(memory: WebAssembly.Memory): View =
    js("new DataView(memory.buffer)")

fun box(): String {
    withScopedMemoryAllocator { allocator ->
        var pointer = allocator.allocate(100)
        val view = getDataViewFromMemory(wasmMemory)

        pointer += 10
        pointer.storeByte(1.toByte())
        if (view.getInt8(pointer.address, true) != 1.toByte()) return "FAIL1"
        view.setInt8(pointer.address, 2.toByte(), true)
        if (pointer.loadByte() != 2.toByte()) return "FAIL2"

        pointer += 10
        pointer.storeShort(3.toShort())
        if (view.getInt16(pointer.address, true) != 3.toShort()) return "FAIL3"
        view.setInt16(pointer.address, 4.toShort(), true)
        if (pointer.loadShort() != 4.toShort()) return "FAIL4"

        pointer += 10
        pointer.storeInt(5)
        if (view.getInt32(pointer.address, true) != 5) return "FAIL5"
        view.setInt32(pointer.address, 6, true)
        if (pointer.loadInt() != 6) return "FAIL6"

        pointer += 10
        pointer.storeLong(123456789000L)
        if (view.getBigInt64(pointer.address, true) != 123456789000L) return "FAIL7"
        view.setBigInt64(pointer.address, 9876542341001L, true)
        if (pointer.loadLong() != 9876542341001L) return "FAIL8"
    }
    return "OK"
}
