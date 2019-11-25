// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: B.java

class B {
    static String test(A x) {
        return A.DefaultImpls.foo(x);
    }
}

// FILE: main.kt

interface A {
    fun foo() = "OK"
}

fun box(): String {
    val result = B.test(object : A {})
    if (result != "OK") return "fail: $result"

    return "OK"
}
