// TARGET_BACKEND: WASM
// ^^ KT-83093

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