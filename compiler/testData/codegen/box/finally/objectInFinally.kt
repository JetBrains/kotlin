var lambda: (() -> String)? = null

fun f() {
    try {
        return
    } finally {
        lambda = { "OK" }
    }
}

fun box(): String {
    f()
    return lambda?.let { it() } ?: "fail"
}