// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: abc/A.java
package abc;
public class A {
    protected A() {}
    protected A(int x) {}
    public A(double x) {}
}

// FILE: main.kt
import abc.*

class B4 : A(1) {
    init {
        <!PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>()
        <!PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>(1)
        A(5.0)
    }

    fun foo() {
        <!PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>()
        <!PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>(1)
        A(5.0)

        object : A() {}
        object : A(1) {}
        object : A(5.0) {}

        class Local : A()
    }
}
