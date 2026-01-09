// FILE: lib.kt
inline fun <R> f(size: Int, block: () -> R): R {
    var result: R
    while (true) {
        result = block()
        if (size == 0) break
    }
    return result
}

// FILE: main.kt
fun computeResult(size: Int) = f(size) {
    42
}

fun box() = if (computeResult(0) == 42) "OK" else "FAIL"
