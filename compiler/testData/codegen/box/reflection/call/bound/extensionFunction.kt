// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE, WASM

// WITH_REFLECT

import kotlin.reflect.*

fun String.foo(x: String) = this + x
fun String?.bar(x: String) = x

fun box() =
        (""::foo).call("O") + (null::bar).call("K")
