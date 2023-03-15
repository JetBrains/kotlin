// TARGET_BACKEND: WASM
// MODULE: main
// FILE: externals.kt

@JsExport
fun sumNumbers(x: Int): Int =
    (0..x).toList().sum()

@JsExport
fun add(x: Int, y: Int): Int =
    x + y

// FILE: entry.mjs

import wasmExports from "./index.mjs"

console.log(wasmExports.add(10, 20));
console.log(wasmExports.sumNumbers(10));