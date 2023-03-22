// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: A.java
public class A {
    protected A() {}
    protected A(int x) {}
    public A(double x) {}
}

// FILE: main.kt

class B4 : A(1) {
    init {
        A()
        A(1)
        A(5.0)
    }

    fun foo() {
        A()
        A(1)
        A(5.0)

        object : A() {}
        object : A(1) {}
        object : A(5.0) {}

        class Local : A()
    }
}
