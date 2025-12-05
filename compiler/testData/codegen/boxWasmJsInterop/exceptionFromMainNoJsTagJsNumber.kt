// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we need to emit index.mjs instead of "$moduleName_$fileName_v5.js" files to unmute this test
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