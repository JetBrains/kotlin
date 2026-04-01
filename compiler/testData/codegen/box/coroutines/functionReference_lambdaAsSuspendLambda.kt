// KT-66093: ClassCastException
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// WITH_STDLIB
// WITH_COROUTINES

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun foo(block: (Continuation<Unit>) -> Any?) {
    block as (suspend () -> Unit)
}

fun box(): String {
    foo {}

    return "OK"
}
