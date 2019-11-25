// IGNORE_BACKEND_FIR: JVM_IR
enum class A { X1, X2 }

fun box(): String {
    when {}
    when (A.X1) {}
    return "OK"
}
