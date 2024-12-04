// WITH_STDLIB
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

inline fun <T> useRef(value: T, f: (T) -> Boolean) = f(value)

fun box(): String {
    val chars = listOf('a') + "-"
    return if (useRef('a', chars::contains)) "OK" else "Failed"
}
