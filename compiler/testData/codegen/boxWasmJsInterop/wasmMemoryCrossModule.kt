// TARGET_BACKEND: WASM

// MODULE: lib
// FILE: lib.kt

@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory

fun getBuffer(memory: WebAssembly.Memory): JsAny = js("memory.buffer")

fun libMemoryBuffer(): JsAny = getBuffer(wasmMemory)

// MODULE: main(lib)
// FILE: main.kt

@file:OptIn(ExperimentalWasmJsInterop::class)

import kotlin.wasm.unsafe.WebAssembly
import kotlin.wasm.unsafe.wasmMemory

fun getMainBuffer(memory: WebAssembly.Memory): JsAny = js("memory.buffer")

fun sameJsObject(a: JsAny, b: JsAny): Boolean = js("a === b")

fun box(): String {
    if (!sameJsObject(getMainBuffer(wasmMemory), libMemoryBuffer())) return "FAIL"
    return "OK"
}
