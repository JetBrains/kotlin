// IGNORE_BACKEND: JVM_IR
fun shouldReturnFalse() : Boolean {
    try {
        return true
    } finally {
        if (true)
            return false
    }
}

fun box(): String =
        if (shouldReturnFalse()) "Failed" else "OK"