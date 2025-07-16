// WITH_STDLIB
// TARGET_BACKEND: WASM

import kotlin.wasm.internal.ExcludedFromCodegen
import kotlin.js.*

external class C : JsAny

fun js(): JsAny = js("123")

@ExcludedFromCodegen
fun testExcludedFunction(): String {
    js().unsafeCast<C>()
    return "OK"
}

fun box(): String {
    return testExcludedFunction()
}