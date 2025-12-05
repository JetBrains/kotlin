// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we need to emit index.mjs instead of "$moduleName_$fileName_v5.js" files to unmute this test
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