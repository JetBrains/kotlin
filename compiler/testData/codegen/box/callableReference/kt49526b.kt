// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND_FIR: JVM_IR
// FIR_STATUS: callable reference type approximation hack not implemented

// WITH_STDLIB

inline fun <T> useRef(value: T, f: (T) -> Boolean) = f(value)

fun box(): String {
    val chars = listOf('a') + "-"
    val ref = chars::contains
    return if (ref('a')) "OK" else "Failed"
}
