// TARGET_BACKEND: WASM

fun throwSomeJsException(): Int = js("{ throw 42; }")

fun withFinally(): Boolean {
    try {
        throwSomeJsException()
        return false
    } finally {
        return true
    }
    return false
}

fun withThrowable(): Boolean {
    try {
        throwSomeJsException()
        return false
    } catch (_: Throwable) {
        return true
    }
    return false
}

fun withJsException(): Boolean {
    try {
        throwSomeJsException()
        return false
    } catch (_: JsException) {
        return true
    }
    return false
}

fun box(): String {

    if (!withFinally()) return "FAIL1"
    if (!withThrowable()) return "FAIL2"
    if (!withJsException()) return "FAIL3"

    return "OK"
}
