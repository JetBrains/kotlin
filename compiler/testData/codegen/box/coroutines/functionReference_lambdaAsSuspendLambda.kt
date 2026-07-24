// IGNORE_BACKEND: WASM_JS, WASM_WASI
// ^^^ KT-66093: ClassCastException
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ KT-82349: ClassCastException
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
