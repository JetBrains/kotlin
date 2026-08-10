// WASM_IGNORE_FOR: os=windows mode=multi-module runner=org.jetbrains.kotlin.wasm.test.WasmJsCodegenCoroutinesStackSwitchingMultiModuleTestGenerated
// ^^^ KT-88235
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

var result = "FAIL"

inline class IC(val s: Any)

var c: Continuation<Any>? = null

suspend fun <T> suspendMe(): T = suspendCoroutine {
    @Suppress("UNCHECKED_CAST")
    c = it as Continuation<Any>
}

interface Base<T : IC?> {
    suspend fun generic(): T
}

class Derived : Base<IC> {
    override suspend fun generic(): IC = suspendMe()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(handleExceptionContinuation {
        result = it.message!!
    })
}

fun box(): String {
    builder {
        val base: Base<*> = Derived()
        base.generic()!!.s as String
    }
    c?.resumeWithException(IllegalStateException("OK"))
    return result
}
