// WITH_STDLIB
// TARGET_BACKEND: WASM

@file:kotlin.wasm.internal.ExcludedFromCodegen

import kotlin.js.*

external class C : JsAny

fun js(): JsAny = js("123")

fun testFunctionInExcludedFile(): String {
    js().unsafeCast<C>()
    return "OK"
}

fun box(): String {
    return testFunctionInExcludedFile()
}