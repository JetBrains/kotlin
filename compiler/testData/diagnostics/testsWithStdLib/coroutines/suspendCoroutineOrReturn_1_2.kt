// !LANGUAGE: -ReleaseCoroutines
// !DIAGNOSTICS: -UNUSED_PARAMETER -EXPERIMENTAL_FEATURE_WARNING
// !CHECK_TYPE
// !WITH_NEW_INFERENCE
// SKIP_TXT

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun noParams(): Unit = suspendCoroutineUninterceptedOrReturn {
        if (hashCode() % 2 == 0) {
            it.resume(Unit)
            COROUTINE_SUSPENDED
        }
        else {
            Unit
        }
    }
    suspend fun yieldString(value: String) = suspendCoroutineUninterceptedOrReturn<Int> {
        it.resume(1)
        it checkType { _<Continuation<Int>>() }
        it.<!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>resume<!>(<!NI;TYPE_MISMATCH!>""<!>)

        // We can return anything here, 'suspendCoroutineUninterceptedOrReturn' is not very type-safe
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
