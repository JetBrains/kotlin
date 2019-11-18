// IGNORE_BACKEND_FIR: JVM_IR
data class A<T>(val t: T)

fun box(): String {
    val h = A<String?>(null).hashCode()
    if (h != 0) return "Fail $h"
    return "OK"
}
