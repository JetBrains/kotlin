// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: SUSPEND_FUNCTION_CAST
// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*

fun check(condition: Boolean, msg: () -> Unit) {
    if (!condition) {
        throw AssertionError(msg())
    }
}

fun fn1(x: Any) {}
suspend fun suspendFn0() {}

val lambda1 = { x: Any -> } as (Any) -> Unit
val suspendLambda0: suspend () -> Unit = {}

fun Any.extFun(a: Any) {}
suspend fun Any.suspendExtFun() {}

class A {
    fun foo(a: Any) {}
    suspend fun suspendFoo() {}
}

inline fun <reified T : suspend () -> Unit> checkReified(noinline x: (Any?) -> Unit) {
    if(x is T) throw IllegalStateException("x is T")
}

fun box(): String {
    val f1 = ::fn1 as Any
    val sf0 = ::suspendFn0 as Any

    val ef = Any::extFun as Any
    val sef = Any::suspendExtFun as Any

    val afoo = A::foo
    val safoo = A::suspendFoo

    fun local1(x: Any) {}
    suspend fun suspendLocal0() {}

    val localFun1 = ::local1 as Any
    val suspendLocalFun0 = ::suspendLocal0 as Any

    check(f1 !is SuspendFunction0<*>) { "Failed: f1 !is SuspendFunction0<*>" }
    check(sf0 is SuspendFunction0<*>) { "Failed: f1 is SuspendFunction0<*>" }
    check(sf0 is Function1<*, *>) { "Failed: suspendF0 is Function1<*, *>" }

    check(lambda1 !is SuspendFunction0<*>) { "Failed: lambda1 !is SuspendFunction0<*>" }
    check(suspendLambda0 is Function1<*, *>) { "Failed: suspendLambda0 is Function1<*, *>" }
    check(suspendLambda0 is SuspendFunction0<*>) { "Failed: suspendLambda0 is SuspendFunction0<*>" }

    check(localFun1 !is SuspendFunction0<*>) { "Failed: localFun1 !is SuspendFunction0<*, *>" }
    check(suspendLocalFun0 is Function1<*, *>) { "Failed: suspendLocalFun0 is Function1<*, *>" }
    check(suspendLocalFun0 is SuspendFunction0<*>) { "Failed: suspendLocalFun0 is SuspendFunction0<*>" }

    check(ef !is SuspendFunction1<*, *>) { "Failed: ef !is SuspendFunction1<*, *>" }
    check(sef is SuspendFunction1<*, *>) { "Failed: sef is SuspendFunction1<*, *>" }
    check(sef is Function2<*, *, *>) { "Failed: sef is Function2<*, *, *>" }

    check(afoo !is SuspendFunction1<*, *>) { "afoo !is SuspendFunction1<*, *>" }
    check(safoo is Function2<*, *, *>) { "safoo is Function2<*, *, *>" }
    check(safoo is SuspendFunction1<*, *>) { "asfoo is SuspendFunction1<*, *>" }

    checkReified<suspend () -> Unit> {}

    return "OK"
}
