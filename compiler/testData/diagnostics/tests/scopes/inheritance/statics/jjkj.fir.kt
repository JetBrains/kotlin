// FILE: I.java

public interface I {
    int a = 1;
}

// FILE: C.java

public class C implements I {
    static int b = 1;
    static void bar() {}
}

// FILE: K.kt

open class K : C()

// FILE: D.java

public class D extends K {
    static int c = 1;
    static void baz() {}
}

// FILE: test.kt

fun test() {
    I.a

    C.a
    C.b
    C.bar()

    K.<!UNRESOLVED_REFERENCE!>a<!>
    K.<!UNRESOLVED_REFERENCE!>b<!>
    K.<!UNRESOLVED_REFERENCE!>bar<!>()

    D.a
    D.b
    D.c
    D.bar()
    D.baz()
}
