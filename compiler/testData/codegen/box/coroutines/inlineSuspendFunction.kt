// WITH_RUNTIME
// WITH_COROUTINES
// WITH_REFLECT
// CHECK_NOT_CALLED: suspendInline_61zpoe$
// CHECK_NOT_CALLED: suspendInline_6r51u9$
// CHECK_NOT_CALLED: suspendInline
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    fun withValue(v: String, x: Continuation<String>) {
        x.resume(v)
    }

    suspend inline fun suspendInline(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
        withValue(v, x)
        COROUTINE_SUSPENDED
    }

    suspend inline fun suspendInline(crossinline b: () -> String): String = suspendInline(b())

    suspend inline fun <reified T : Any> suspendInline(): String = suspendInline({ T::class.simpleName!! })
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

class OK

fun box(): String {
    var result = ""

    builder {
        result = suspendInline("56")
        if (result != "56") throw RuntimeException("fail 1")

        result = suspendInline { "57" }
        if (result != "57") throw RuntimeException("fail 2")

        result = suspendInline<OK>()
    }

    return result
}
