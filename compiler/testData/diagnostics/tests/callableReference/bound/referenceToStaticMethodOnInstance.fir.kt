// FILE: A.java

public class A {
    public static void test() {}
}

// FILE: test.kt

enum class E { EN }

fun test() {
    <!UNRESOLVED_REFERENCE!>A()::test<!>
    <!UNRESOLVED_REFERENCE!>E.EN::valueOf<!>
}
