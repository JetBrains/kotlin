// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR

class A {
    val o = "O"
}
class B {
    val k = "K"
}

context(B) fun A.f(a: Any, b: Any) = o + k

fun B.g(a: A): String {
    with (a) {
        return f(1, "2")
    }
}

fun box(): String {
    return B().g(A())
}