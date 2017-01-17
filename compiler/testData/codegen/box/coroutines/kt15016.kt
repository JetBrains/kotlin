// WITH_RUNTIME
// WITH_COROUTINES

import kotlin.coroutines.intrinsics.SUSPENDED_MARKER
import kotlin.coroutines.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.startCoroutine

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

suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
    x.resume("OK")
    SUSPENDED_MARKER
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
