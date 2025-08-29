// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL

// FILE: main.kt
fun throwNumberFromJs(): Int = js("{ throw 42; }")

fun main() {
    throwNumberFromJs()
}

fun box() = "OK"

// FILE: entry.mjs
let nothrow = false;
try {
    const m = await import("./index.mjs")
    nothrow = true
} catch(e) {
    if (typeof e !== "number") {
        throw Error("Expected number")
    }
    if (e !== 42) {
        throw Error("Wrong value")
    }
}
if (nothrow) throw Error("Unexpected successful call");