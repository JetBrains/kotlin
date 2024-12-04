// TARGET_BACKEND: WASM
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

// MODULE: export_invalid_name_function
// FILE: lib.kt

@JsExport
fun `@do something like-this`(`test value`: Int): Int = `test value`

// FILE: entry.mjs
import { '@do something like-this' as doSomething } from "./index.mjs"

var value = doSomething(42)

if (value !== 42)
    throw "false: expect exproted function '@do something like-this' to return 42 but it equals " + value