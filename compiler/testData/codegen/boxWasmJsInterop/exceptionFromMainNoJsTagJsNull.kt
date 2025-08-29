// TARGET_BACKEND: WASM
// WASM_NO_JS_TAG

// FILE: main.kt
fun throwNullFromJs(): Int = js("{ throw null; }")

fun main() { throwNullFromJs() }

fun box() = "OK"

// FILE: entry.mjs
let nothrow = false;
try {
    await import("./index.mjs");
    nothrow = true;
} catch (e) {
    if (e !== null) {
        throw Error("Expected null");
    }
}
if (nothrow) throw Error("Unexpected successful call");