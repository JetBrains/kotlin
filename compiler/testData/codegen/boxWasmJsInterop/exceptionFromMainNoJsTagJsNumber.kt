// TARGET_BACKEND: WASM
// WASM_NO_JS_TAG

// FILE: main.kt
fun throwNumberFromJs(): Int = js("{ throw 42; }")

fun main() { throwNumberFromJs() }

fun box() = "OK"

// FILE: entry.mjs
let nothrow = false;
try {
    await import("./index.mjs");
    nothrow = true;
} catch (e) {
    if (typeof e !== "number") {
        throw Error("Expected number");
    }
    if (e !== 42) {
        throw Error("Wrong number");
    }
}
if (nothrow) throw Error("Unexpected successful call");