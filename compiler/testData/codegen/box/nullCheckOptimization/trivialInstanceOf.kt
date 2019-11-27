// IGNORE_BACKEND_FIR: JVM_IR
sealed class A {
    class B : A()

    class C : A()
}

inline fun foo(): A = A.B()

fun box(): String {
    val a: A = foo()
    val b: Boolean
    when (a) {
        is A.B -> b = true
        is A.C -> b = false
    }
    return if (b) "OK" else "FAIL"
}