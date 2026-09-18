// WITH_STDLIB
// WITH_COROUTINES

// On Wasm, structurally identical suspend lambda classes created by WasmSuspendLambdaMergingLowering
// are deduplicated at link time, including across module boundaries. Both the classes and their
// members are keyed by the (structural) class name, so an object created by the canonical constructor
// of one module must be accepted by the `doResume` bridge of the other one.

// MODULE: lib
// FILE: lib.kt

suspend fun id(x: Any): Any = x

fun fromLib(x: Any): suspend () -> Any = { id(x); x }

// MODULE: main(lib)
// FILE: main.kt

import kotlin.coroutines.*

fun fromMain(x: Any): suspend () -> Any = { id(x); x }

fun runCoroutine(c: suspend () -> Any): Any? {
    var result: Any? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

fun box(): String {
    val o = runCoroutine(fromLib("O"))
    val k = runCoroutine(fromMain("K"))
    return "$o$k"
}
