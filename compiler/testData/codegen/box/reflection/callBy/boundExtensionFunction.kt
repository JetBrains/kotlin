// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.test.assertEquals

fun String.extFun(k: String, s: String = "") = this + k + s

fun box(): String {
    val sExtFun = "O"::extFun
    return sExtFun.callBy(mapOf(sExtFun.parameters[0] to "K"))
}
