// !DIAGNOSTICS: -UNUSED_PARAMETER
// !CHECK_TYPE
class Controller {
    suspend fun noParams(): Unit = suspendWithCurrentContinuation {
        if (hashCode() <!DEPRECATED_BINARY_MOD_AS_REM!>%<!> 2 == 0) {
        it.resume(Unit)
        Suspend
    }
        else {
        Unit
    }
    }
    suspend fun yieldString(value: String) = suspendWithCurrentContinuation<Int> {
        it.resume(1)
        it checkType { _<Continuation<Int>>() }
        it.resume(<!TYPE_MISMATCH!>""<!>)

        // We can return anything here, 'suspendWithCurrentContinuation' is not very type-safe
        // Also we can call resume and then return the value too, but it's still just our problem
        "Not-int"
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {}

fun test() {
    builder {
        noParams() checkType { _<Unit>() }
        yieldString("abc") checkType { _<Int>() }
    }
}
