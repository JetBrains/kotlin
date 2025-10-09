// TARGET_BACKEND: WASM
// WASM_NO_JS_TAG
// WASM_FAILS_IN_SINGLE_MODULE_MODE

// FILE: main.kt
fun runWithException() {
    throw AssertionError("Some random exception")
}

fun main() {
    runWithException()
}

fun box() = "OK"

// FILE: entry.mjs
let nothrow = false;
try {
    const m = await import("./index.mjs")
    nothrow = true
} catch(e) {
    if (!(e instanceof WebAssembly.Exception)) {
        throw "Expected to have WebAssembly.Exception, but '" + e.name +"' ('" + e.constructor.name + "') was received"
    }
}

if (nothrow) throw Error("Unexpected successful call");
