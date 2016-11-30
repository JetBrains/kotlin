// !DIAGNOSTICS: -UNUSED_PARAMETER

suspend fun baz() = 1

suspend fun foo() {}

suspend fun bar0() = baz()
suspend fun bar01(): Int {
    return baz()
}

suspend fun bar1() {
    return if (1.hashCode() > 0) {
        foo()
    }
    else suspendWithCurrentContinuation { x: Continuation<Unit> -> }
}

suspend fun bar2() =
        if (1.hashCode() > 0) {
            foo()
        }
        else suspendWithCurrentContinuation { x: Continuation<Unit> -> }

suspend fun bar3() =
        when {
            true -> { foo() }
            else -> suspendWithCurrentContinuation { x: Continuation<Unit> -> }
        }
