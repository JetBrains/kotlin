// TARGET_BACKEND: WASM
// ^^ For JS_IR and JS_IR_ES6, we need to emit index.mjs instead of "$moduleName_$fileName_v5.js" files to unmute this test

// KT-71691

// FILE: main.kt
val globalVal: Int = throw Exception("Some")

fun main() {
    result = "OK"
}

fun box() = "OK"

// FILE: result.kt
var result = "not OK"

@JsExport
fun getResult() = result

// FILE: entry.mjs
let nothrow = false;
try {
    const m = await import("./index.mjs")
    nothrow = true
} catch(e) {
    if (!(e instanceof Error)) {
        throw Error("Expected instance of Error, but '" + e.name +"' ('" + e.constructor.name + "') was received")
    }

    if (e.name !== "Exception" && e.message == "Some") {
        throw Error("Wrong e.name = '" + e.name + "'")
    }
}

if (nothrow) throw Error("Unexpected successful call");
