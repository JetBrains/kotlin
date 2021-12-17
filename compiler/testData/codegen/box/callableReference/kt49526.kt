// IGNORE_BACKEND_FIR: JVM_IR
// FIR_STATUS: callable reference type approximation hack not implemented

// WITH_STDLIB
// CHECK_BYTECODE_LISTING

inline fun <T> useRef(value: T, f: (T) -> Boolean) = f(value)

fun box(): String {
    val chars = listOf('a') + "-"
    return if (useRef('a', chars::contains)) "OK" else "Failed"
}
