// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we need to emit index.mjs instead of "$moduleName_$fileName_v5.js" files to unmute this test
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL

// FILE: main.kt
fun throwNullFromJs(): Int = js("{ throw null; }")

fun main() {
    throwNullFromJs()
}

fun box() = "OK"

// FILE: entry.mjs
let nothrow = false;
try {
    const m = await import("./index.mjs")
    nothrow = true
} catch(e) {
    if (e !== null) {
        throw Error("Expected null")
    }
}
if (nothrow) throw Error("Unexpected successful call");