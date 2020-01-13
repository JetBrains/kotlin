// FILE: A.java
public class A {
    public static void foo() {}
}

// FILE: 1.kt
open class B: A()

// FILE: C.java
public class C extends B {
    public static void bar() {}
}

// FILE: 2.kt
class D: C() {
    init {
        foo()
        A.foo()
        B.<!UNRESOLVED_REFERENCE!>foo<!>()
        C.foo()
        D.<!UNRESOLVED_REFERENCE!>foo<!>()

        bar()
        C.bar()
        D.<!UNRESOLVED_REFERENCE!>bar<!>()
    }
}
