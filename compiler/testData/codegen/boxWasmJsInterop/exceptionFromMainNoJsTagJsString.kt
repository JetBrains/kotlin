// TARGET_BACKEND: WASM
// WASM_NO_JS_TAG

// FILE: main.kt
fun throwSomeJsPrimitive(): Int = js("{ throw 'Test'; }")

fun main() {
    throwSomeJsPrimitive()
}

fun box() = "OK"

// FILE: entry.mjs
let nothrow = false;
try {
    await import("./index.mjs");
    nothrow = true;
} catch (e) {
    if (typeof e !== "string") {
        throw Error("Expected string");
    }
    if (e !== "Test") {
        throw Error("Wrong string");
    }
}
if (nothrow) throw Error("Unexpected successful call");