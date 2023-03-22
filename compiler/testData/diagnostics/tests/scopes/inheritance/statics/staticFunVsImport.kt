// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: A.java
public class A {
    static void foo() {}
    static int bar() {return 1;}
    void nonStatic1() {}
    void nonStatic2() {}
}

// FILE: B.java
public class B extends A {}

// FILE: C.java
public class C {
    static void bar() {}
}

// FILE: 1.kt
import A.foo
import B.bar
import A.<!CANNOT_BE_IMPORTED!>nonStatic1<!>
import B.<!CANNOT_BE_IMPORTED!>nonStatic2<!>

class E: A() {
    init {
        foo()
        bar()
    }
}

class F: B() {
    init {
        foo()
        bar()
    }
}

// FILE: 2.kt
import C.bar

class Z: A() {
    init {
        val a: Int = bar()
    }
}

// FILE: 3.kt
import C.*

class Q: A() {
    init {
        val a: Int = bar()
    }
}