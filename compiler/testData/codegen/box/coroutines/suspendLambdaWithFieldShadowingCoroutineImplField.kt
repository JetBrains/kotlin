// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// `result`, `state` and `exception` are names CoroutineImpl declares fields for. A suspend
// function parameter with one of those names becomes a field of the same name on the coroutine
// class, so selecting the class's own fields by name would drop it as if it were the inherited
// base field. The field would then be missing from the merged class and from the field mapping,
// and the moved state machine body would keep reading it off the original coroutine class while
// its receiver has already been cast to the merged class. On Wasm that shows up either as a
// validation failure,
//   struct.get[0] expected type (ref null N), found ref.cast null of type (ref null M)
// when only some of the fields are dropped, or as an IndexOutOfBoundsException while rewriting
// the constructor call when every dropped field leaves the merged class short of arguments.

suspend fun shadowsResult(result: () -> String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(result())
    COROUTINE_SUSPENDED
}

suspend fun shadowsStateAndException(state: () -> String, exception: () -> String): String =
    suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(state() + exception())
        COROUTINE_SUSPENDED
    }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var log = ""

    builder {
        fun o() = "O"
        log += shadowsResult(::o)
    }
    builder {
        fun k() = "K"
        fun bang() = "!"
        log += shadowsStateAndException(::k, ::bang)
    }

    if (log != "OK!") return "FAIL: $log"
    return "OK"
}
