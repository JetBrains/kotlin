// FILE: A.java
public class A {
    public static void foo() {}
}

// FILE: B.java
public class B extends A {
    public static void bar() {}
}

// FILE: 1.kt
open class X: A() {
    init {
        foo()
        A.foo()
    }
}

open class Y: B() {
    init {
        foo()
        A.foo()
        B.foo()

        bar()
        B.bar()
    }
}

class XN: X() {
    init {
        foo()
        A.foo()
        X.<!UNRESOLVED_REFERENCE!>foo<!>()
        XN.<!UNRESOLVED_REFERENCE!>foo<!>()
    }
}

class YN: Y() {
    init {
        foo()
        A.foo()
        Y.<!UNRESOLVED_REFERENCE!>foo<!>()
        YN.<!UNRESOLVED_REFERENCE!>foo<!>()

        bar()
        B.bar()
        Y.<!UNRESOLVED_REFERENCE!>bar<!>()
        YN.<!UNRESOLVED_REFERENCE!>bar<!>()
    }
}
