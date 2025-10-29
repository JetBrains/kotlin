// LANGUAGE: +ContextParameters
// IGNORE_BACKEND_K1: ANY
// WITH_STDLIB
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun box(): String {
    withFooBar { fooBarBaz() }
    return "OK"
}

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

class Foo
class Bar
class Baz

context(_: Foo, _: Bar)
fun Baz.fooBarBaz() { }

fun withFooBar(block: suspend context(Foo, Bar) Baz.() -> Unit) {
    val foo = Foo()
    val bar = Bar()
    val baz = Baz()
    runBlocking { block(foo, bar, baz) }
}