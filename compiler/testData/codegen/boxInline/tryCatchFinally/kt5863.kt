// FILE: 1.kt

inline fun <R> performWithFinally(finally: () -> R) : R {
    try {
        throw RuntimeException("1")
    } catch (e: RuntimeException) {
        throw RuntimeException("2")
    } finally {
        return finally()
    }
}

// FILE: 2.kt

inline fun test2Inline() = performWithFinally { "OK" }

fun box(): String {
    return test2Inline()
}
