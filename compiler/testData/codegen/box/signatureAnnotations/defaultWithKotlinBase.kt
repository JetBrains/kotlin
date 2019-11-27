// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: A.kt
open class A {
    open fun x(x: Int = foo()) = x
    private fun foo() = 42
}

// FILE: B.java
public class B extends A {
    public int x(int i) {
        return i + 1;
    }
}

// FILE: box.kt
fun box(): String {
    if (B().x() != 43) {
        return "FAIL"
    }

    return "OK"
}