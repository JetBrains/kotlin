// FILE: A.java

public class A {
    public static void foo(A... values) {}
}

// FILE: test.kt

fun test(vararg values: A) {
    A.foo(*values)
    A.foo(<!ARGUMENT_TYPE_MISMATCH!>values<!>)
}
