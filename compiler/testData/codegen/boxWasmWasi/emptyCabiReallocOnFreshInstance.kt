// KT-90107
// MODULE: main
// FILE: main.kt

import kotlin.wasm.WasmExport
import kotlin.wasm.unsafe.withScopedMemoryAllocator

// Allocate a zero-size allocation, which must still grow memory, otherwise the host VM might trap
@OptIn(kotlin.wasm.unsafe.UnsafeWasmMemoryApi::class, kotlin.wasm.ExperimentalWasmInterop::class)
@WasmExport
fun allocateZero(): Int = withScopedMemoryAllocator { it.allocate(0).address.toInt() }

fun box() = "OK"

// FILE: entry.mjs

import { __ALL_EXPORTS as exports } from "./index.mjs" // "index" is a value of `const val WASM_BASE_FILE_NAME`

// The canonical ABI specifies that cabi_realloc must return a pointer <= len(memory). This must also be the case for normal allocations,
// as they can be handed to the host, which might then enforce pointer <= len(memory) too. So even for zero-size allocations, memory must be
// grown to ensure their result address is <= len(memory).

function check(what, ptr) {
    const memorySize = exports.memory.buffer.byteLength
    if (ptr % 8 !== 0) throw `${what} returned ${ptr}, which is not 8-byte aligned`
    if (ptr > memorySize) throw `${what} returned ${ptr}, beyond end of memory (${memorySize} bytes)`
}

// allocateZero() is checked first, as it is the lower-level path (cabi_realloc is built on top of it),
// so a fix that covers only `cabi_realloc` would be caught, even if the module is not reset to a fresh (0 memory) instance in between the checks.
check("allocateZero()", exports.allocateZero())
check("cabi_realloc(0, 0, 1, 0)", exports.cabi_realloc(0, 0, 1, 0))
