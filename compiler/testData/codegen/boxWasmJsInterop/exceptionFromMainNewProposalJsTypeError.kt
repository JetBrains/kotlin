// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we need to emit index.mjs instead of "$moduleName_$fileName_v5.js" files to unmute this test
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL

// FILE: main.kt
fun throwJsTypeError(): Int = js("{ throw new TypeError('Type problem'); }")

fun main() {
    throwJsTypeError()
}

fun box() = "OK"

// FILE: entry.mjs
let nothrow = false;
try {
    const m = await import("./index.mjs")
    nothrow = true
} catch(e) {
    if (!(e instanceof Error)) {
        throw Error("Expected Error")
    }
    if (!(e instanceof TypeError)) {
        throw Error("Expected TypeError")
    }
    if (e.name !== "TypeError") {
        throw Error("Wrong name")
    }
    if (!String(e.message || "").startsWith("Type problem")) {
        throw Error("Wrong message")
    }
}
if (nothrow) throw Error("Unexpected successful call");