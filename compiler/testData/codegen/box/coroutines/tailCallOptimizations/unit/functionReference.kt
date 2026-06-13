// IGNORE_BACKEND: JS_IR_ES6
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-JS:2.3
// ^^^ Running after compilation with Kotlin 2.3 gives:
//     RuntimeError: illegal cast
//        at <main>.$invokeCOROUTINE$.doResume (wasm://wasm/<main>-002e8cb2:wasm-function[3874]:0x4c55a)
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

object Dummy

suspend fun suspect(): Dummy = suspendCoroutineUninterceptedOrReturn {
    x -> x.resume(Dummy)
    COROUTINE_SUSPENDED
}

fun box(): String {
    var res: Any? = null
    suspend {
        res = (::suspect as suspend () -> Unit)()
    }.startCoroutine(EmptyContinuation)
    return if (res != Unit) "$res" else "OK"
}
