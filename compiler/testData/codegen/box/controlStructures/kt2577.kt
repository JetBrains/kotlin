
fun foo(): Int {
    try {
    } finally {
        try {
            return 1
        } catch (e: Throwable) {
            return 2
        }
    }
}

fun box() = if (foo() == 1) "OK" else "Fail"
