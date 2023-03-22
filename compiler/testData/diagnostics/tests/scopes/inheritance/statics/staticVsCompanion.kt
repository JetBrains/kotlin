// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java
public class A {
    static void foo() {}
}

// FILE: 1.kt
open class B : A() {
    companion object {
        fun foo() = 1
    }

    init {
        val a: Int = foo()
    }
}

class C: B() {
    init {
        val a: Int = foo()
    }
}

// FILE: X.java
public class X extends B {
    static double foo() {
        return 1.0;
    }
}

// FILE: 2.kt
class Y: X() {
    init {
        val a: Double = foo()
    }
}
