// TARGET_BACKEND: WASM
// ^^ KT-83093

// FILE: main.kt
fun throwCustomNamedJsError(): Int = js("""
  {
    const e = new Error("Custom error");
    e.name = "MyCustomError";
    throw e;
  }
""")

fun main() {
    throwCustomNamedJsError()
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
    if (e.name !== "MyCustomError") {
        throw Error("Wrong name")
    }
    if (e.message !== "Custom error") {
        throw Error("Wrong message")
    }
}
if (nothrow) throw Error("Unexpected successful call");