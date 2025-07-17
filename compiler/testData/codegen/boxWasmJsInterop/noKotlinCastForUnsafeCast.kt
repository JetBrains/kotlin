// TARGET_BACKEND: WASM
// KT-79317

import kotlin.js.*

external class C : JsAny
fun jsVal(): JsAny = js("123")

fun box(): String {
    jsVal().unsafeCast<C>()
    return "OK"
}
