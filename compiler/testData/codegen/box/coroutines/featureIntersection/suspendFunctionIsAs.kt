// IGNORE_BACKEND: JS_IR, JS
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// LANGUAGE_VERSION: 1.3

import helpers.*
import kotlin.coroutines.*

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

    assert(f1 !is SuspendFunction0<*>) { "Failed: f1 !is SuspendFunction0<*>" }
    assert(sf0 is SuspendFunction0<*>) { "Failed: f1 is SuspendFunction0<*>" }
    assert(sf0 is Function1<*, *>) { "Failed: suspendF0 is Function1<*, *>" }

    assert(lambda1 !is SuspendFunction0<*>) { "Failed: lambda1 !is SuspendFunction0<*>" }
    assert(suspendLambda0 is Function1<*, *>) { "Failed: suspendLambda0 is Function1<*, *>" }
    assert(suspendLambda0 is SuspendFunction0<*>) { "Failed: suspendLambda0 is SuspendFunction0<*>" }

    assert(localFun1 !is SuspendFunction0<*>) { "Failed: localFun1 !is SuspendFunction0<*, *>" }
    assert(suspendLocalFun0 is Function1<*, *>) { "Failed: suspendLocalFun0 is Function1<*, *>" }
    assert(suspendLocalFun0 is SuspendFunction0<*>) { "Failed: suspendLocalFun0 is SuspendFunction0<*>" }

    assert(ef !is SuspendFunction1<*, *>) { "Failed: ef !is SuspendFunction1<*, *>" }
    assert(sef is SuspendFunction1<*, *>) { "Failed: sef is SuspendFunction1<*, *>" }
    assert(sef is Function2<*, *, *>) { "Failed: sef is Function2<*, *, *>" }

    assert(afoo !is SuspendFunction1<*, *>) { "afoo !is SuspendFunction1<*, *>" }
    assert(safoo is Function2<*, *, *>) { "safoo is Function2<*, *, *>" }
    assert(safoo is SuspendFunction1<*, *>) { "asfoo is SuspendFunction1<*, *>" }

    checkReified<suspend () -> Unit> {}

    return "OK"
}