// FILE: J.java
public class J implements I {
    @Override
    public int foo(int a, int b) {
        return a + b;
    }
}

// FILE: J2.java
public class J2 implements I {
    @Override
    public int foo(int ax, int bx) {
        return ax + bx;
    }
}

// FILE: J3.java
public class J3 implements I, I2 {
    @Override
    public int foo(int ax, int bx) {
        return ax + bx;
    }
}

// FILE: J4.java
public class J4 extends J2 {
    @Override
    public int foo(int axx, int bxx) {
        return a2 + b2;
    }
}

// FILE: I.kt
interface I {
    fun foo(a: Int, b: Int): Int
}

interface I2 {
    fun foo(aa: Int, bb: Int): Int
}

fun test() {
    J().foo(1, b = 2)

    J2().foo(1, b = 2)
    J2().foo(1, <!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>bx<!> = 2)<!>

    J3().foo(1, b = 2) // K1 bug, fixed in K2 (KT-67546)
    J3().foo(1, <!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>bb<!> = 2)<!>
    J3().foo(1, <!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>bx<!> = 2)<!>

    J4().foo(1, b = 2)
    J4().foo(1, <!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>bx<!> = 2)<!>
    J4().foo(1, <!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>bxx<!> = 2)<!>
}
