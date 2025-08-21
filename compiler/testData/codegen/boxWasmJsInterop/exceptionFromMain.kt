// TARGET_BACKEND: WASM

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
    if (!(e instanceof Error)) {
        throw Error("Expected instance of Error, but '" + e.name +"' ('" + e.constructor.name + "') was received")
    }

    if (e.name !== "AssertionError" ) {
        throw Error("Wrong e.name = '" + e.name + "'")
    }
}

if (nothrow) throw Error("Unexpected successful call");
