// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun noParams(): Unit = suspendCoroutineOrReturn {
        if (hashCode() % 2 == 0) {
            it.resume(Unit)
            COROUTINE_SUSPENDED
        }
        else {
            Unit
        }
    }
    suspend fun yieldString(value: String) = suspendCoroutineOrReturn<Int> {
        it.resume(1)
        it checkType { _<Continuation<Int>>() }
        it.resume(<!TYPE_MISMATCH!>""<!>)

        // We can return anything here, 'suspendCoroutineOrReturn' is not very type-safe
        // Also we can call resume and then return the value too, but it's still just our problem
        "Not-int"
    }
}

fun builder(c: suspend Controller.() -> Unit) {}

fun test() {
    builder {
        noParams() checkType { _<Unit>() }
        yieldString("abc") checkType { _<Int>() }
    }
}
