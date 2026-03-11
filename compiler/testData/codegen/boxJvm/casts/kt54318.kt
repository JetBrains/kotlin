// TARGET_BACKEND: JVM_IR

fun f(b: Int): Long? {
    return try {
        null
    } catch (e: Throwable) {
        if (b == 0)
            throw e
        else {
            null
        }
    }
}

fun box(): String {
    f(0)
    f(1)
    return "OK"
}
