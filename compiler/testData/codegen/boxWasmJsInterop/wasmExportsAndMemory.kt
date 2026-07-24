// TARGET_BACKEND: WASM

@file:OptIn(UnsafeWasmMemoryApi::class, ExperimentalWasmJsInterop::class)

import kotlin.wasm.unsafe.*

fun overloadConsoleError(): Unit = js("console.error = function(x) { globalThis.wasError = x }")
fun getConsoleError(): String = js("globalThis.wasError")
fun resetConsoleError(): String = js("globalThis.wasError = ''")

fun getWasmExports(): JsAny = js("wasmExports")
fun getGetSomeValue(exprts: JsAny): JsAny = js("exprts.someValue")

fun getMemory(exprts: JsAny): WebAssembly.Memory = js("exprts.memory")
fun getBuffer(mem: WebAssembly.Memory): JsAny = js("mem.buffer")

const val messageMemory = "Accessing `memory` via `wasmExports` is deprecated. Use `kotlin.wasm.unsafe.wasmMemory` or update dependencies. Read more: https://kotl.in/vr3szr"
const val messageWasmExports = "Accessing exports via `wasmExports` is no longer supported. Remove usages or update dependencies. Read more: https://kotl.in/vr3szr"

fun box(): String {

    overloadConsoleError()
    resetConsoleError()

    val wasmExports = getWasmExports()
    if (getConsoleError() != "") return "FAIL1"

    val memory = getMemory(wasmExports)
    if (getConsoleError() != "") return "FAIL2"

    val buffer = getBuffer(memory);
    if (getConsoleError() != messageMemory) return "FAIL3"
    resetConsoleError()

    val buffer2 = getBuffer(memory); //Second time - no error
    if (getConsoleError() != "") return "FAIL4"

    if (buffer !== buffer2) return "FAIL5"

    val stdlibMemory = wasmMemory;
    if (getConsoleError() != "") return "FAIL6"

    val stdlibBuffer = getBuffer(memory);
    if (getConsoleError() != "") return "FAIL7"

    if (buffer !== stdlibBuffer) return "FAIL8"

    try {
        getGetSomeValue(wasmExports)
    } catch(e: JsException) {
        if (e.message == messageWasmExports) return "OK"
    }

    return "FAIL9"
}

