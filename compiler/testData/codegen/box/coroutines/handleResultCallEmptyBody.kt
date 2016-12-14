// WITH_RUNTIME
// WITH_COROUTINES

fun builder(c: @Suspend() (() -> Unit)): String {
    var ok = false
    c.startCoroutine(handleResultContinuation {
        ok = true
    })
    if (!ok) throw RuntimeException("Was not called")
    return "OK"
}

fun unitFun() {}

fun box(): String {
    return builder {}
}
