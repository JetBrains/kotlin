// IGNORE_BACKEND_FIR: JVM_IR
data class A(val v: Any?)

data class B<T>(val v: T)

fun box(): String {
    val a1 = A(null)
    val a2 = A("")
    if (a1 == a2 || a2 == a1) return "Fail 1"

    val b1 = B(null)
    val b2 = B("")
    if (b1 == b2 || b2 == b1) return "Fail 2"

    return "OK"
}
