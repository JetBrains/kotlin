// WITH_RUNTIME
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var p: Int = 5846814
private suspend fun withoutInline() {
    val c = { c: Continuation<Unit> ->
        if (p > 52158) Unit else COROUTINE_SUSPENDED
    }

    return suspendCoroutineUninterceptedOrReturn(c)
}

private suspend fun withInline() {
    return suspendCoroutineUninterceptedOrReturn { c ->
        if (p > 52158) Unit else COROUTINE_SUSPENDED
    }
}
