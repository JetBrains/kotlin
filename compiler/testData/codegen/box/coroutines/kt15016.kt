// WITH_STDLIB
// WITH_COROUTINES
import helpers.*

import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.*

class Bar(val x: Any)
inline fun Any.map(transform: (Any) -> Any) {
    when (this) {
        is Foo -> Bar(transform(value))
    }
}

class Foo(val value: Any) {
    companion object {
        inline fun of(f: () -> Unit): Any = try {
            Foo(f())
        } catch(ex: Exception) {
            Foo(Unit)
        }
    }
}

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        Foo.of {

        }.map {
            result = suspendHere()
            Unit
        }
    }

    return result
}
