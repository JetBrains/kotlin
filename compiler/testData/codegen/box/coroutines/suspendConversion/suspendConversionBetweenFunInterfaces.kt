// WITH_STDLIB
// WITH_COROUTINES
// ISSUE: KT-67933
// IGNORE_BACKEND_K1: JVM
// ^K1 with old backend reports FUN_INTERFACE_WITH_SUSPEND_FUNCTION
// IGNORE_BACKEND: JS, JS_IR, JS_IR_ES6
// ^ IMPLEMENTING_FUNCTION_INTERFACE: Implementing function interface is prohibited in JavaScript
// JVM_ABI_K1_K2_DIFF: KT-62855

import helpers.*
import kotlin.coroutines.*

interface Flow<out T> {
    suspend fun collect(c: FlowCollector<T>)
}

fun interface FlowCollector<in T> {
    suspend fun emit(x: T)
}

fun interface Fn<in T> : (T) -> Unit {
    override fun invoke(x: T)
}

suspend fun <T> Flow<T>.foo(
    function: (T) -> Unit,
    funInterface: Fn<T>,
) {
    collect(function)
    collect(funInterface)
}

suspend fun Flow<String>.useFoo() {
    foo(
        {},
        object : Fn<String> {
            override fun invoke(x: String) {}
        }
    )
}

suspend fun use(f: suspend Flow<String>.() -> Unit) {
    val flow = object : Flow<String> {
        suspend override fun collect(c: FlowCollector<String>) {}
    }
    flow.f()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        use { useFoo() }
    }
    return "OK"
}
