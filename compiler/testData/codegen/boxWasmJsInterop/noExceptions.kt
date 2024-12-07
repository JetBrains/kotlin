// TARGET_BACKEND: WASM
// DISABLE_WASM_EXCEPTION_HANDLING
// MODULE: main
// FILE: foo.kt

@JsExport
fun sumNumbers(x: Int): Int =
    (0..x).toList().sum()

@JsExport
fun add(x: Int, y: Int): Int =
    x + y

@JsExport
fun throws() {
    try {
        error("Test Error")
    } catch (e: Throwable) {
        // This code normally catches the error,
        // but with exception handling disabled it should fail with trap
    }
}


// FILE: entry.mjs

import { add, sumNumbers, throws } from "./index.mjs";

if (add(10, 20) !== 30) {
    throw "Fail add";
}

if (sumNumbers(10) !== 55) {
    throw "Fail sumNumbers";
}

var thrown = false;
try {
    throws();
} catch(e) {
    thrown = true;
}

if (!thrown) {
    throw "Fail exception was not thrown";
}