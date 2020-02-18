// FILE: 1.kt
inline fun runReturning(f: () -> Nothing): Nothing = f()

// FILE: 2.kt
fun box(): String {
    var r = ""
    val x = try {
        Array<String>(1) {
            try {
                runReturning { throw RuntimeException() }
            } finally {
                r += "1"
            }
        }[0]
    } catch (e: Throwable) {
        r += "2"
        "OK"
    } finally {
        r += "3"
    }
    return if (r == "123") x else r
}
