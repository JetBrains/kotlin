// !LANGUAGE: +ReleaseCoroutines
// !API_VERSION: 1.3
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS, NATIVE, JS_IR
// IGNORE_LIGHT_ANALYSIS

// WITH_RUNTIME
// WITH_COROUTINES

import kotlin.test.assertEquals
import kotlin.jvm.internal.FunctionBase
import helpers.*
import kotlin.coroutines.*

suspend fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun test(f: Function<*>, arity: Int) {
    assertEquals(arity, (f as FunctionBase).arity)
}

suspend fun foo(s: String, i: Int) {}
class A {
    suspend fun bar(s: String, i: Int) {}
}

suspend fun Double.baz(s: String, i: Int) {}

fun box(): String {
    test(::foo, 2 + 1)
    test(A::bar, 3 + 1)
    test(Double::baz, 3 + 1)

    test(::box, 0)

    suspend fun local(x: Int) {}
    test(::local, 1 + 1)

    // TODO: Uncomment when `suspend fun` will be supported
    // test(suspend fun(s: String) = s, 1)
    // test(suspend fun(){}, 0)
    test(suspend {}, 1)

    return "OK"
}
