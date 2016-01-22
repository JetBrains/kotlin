inline fun exit(): Nothing =
        throw RuntimeException() // ATHROW

fun box(): String {
    val a: String
    try {
        a = "OK"
    }
    catch (e: Exception) {
        exit() // ATHROW inlined
        // no ATHROW (removed as dead code)
    }
    return a
}

// 2 ATHROW