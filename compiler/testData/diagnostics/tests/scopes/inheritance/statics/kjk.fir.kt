// FILE: K.kt

open class K {
    companion object {
        fun foo() {}
    }
}

// FILE: D.java

class D extends K {
    static int b = 1;
    static void bar() {}
}

// FILE: K.kt

class K2 {
    companion object {
        fun baz() {}
    }
}

// FILE: test.kt

fun test() {
    K.foo()

    D.b
    D.bar()
    D.<!UNRESOLVED_REFERENCE!>foo<!>()

    K2.<!UNRESOLVED_REFERENCE!>b<!>
    K2.<!UNRESOLVED_REFERENCE!>bar<!>()
    K2.<!UNRESOLVED_REFERENCE!>foo<!>()
    K2.baz()
}
