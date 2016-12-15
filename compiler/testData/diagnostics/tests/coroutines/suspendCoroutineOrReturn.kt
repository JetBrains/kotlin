// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
import kotlin.coroutines.*

class Controller {
    suspend fun noParams(): Unit = CoroutineIntrinsics.suspendCoroutineOrReturn {
        if (hashCode() % 2 == 0) {
            it.resume(Unit)
            CoroutineIntrinsics.SUSPENDED
        }
        else {
            Unit
        }
    }
    suspend fun yieldString(value: String) = CoroutineIntrinsics.suspendCoroutineOrReturn<Int> {
        it.resume(1)
        it checkType { _<Continuation<Int>>() }
        it.resume(<!TYPE_MISMATCH!>""<!>)

        // We can return anything here, 'CoroutineIntrinsics.suspendCoroutineOrReturn' is not very type-safe
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
