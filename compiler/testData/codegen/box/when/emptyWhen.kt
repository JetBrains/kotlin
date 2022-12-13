// KT-55459
// IGNORE_BACKEND_K2: NATIVE

enum class A { X1, X2 }

fun box(): String {
    when {}
    when (A.X1) { else -> {} }
    return "OK"
}
