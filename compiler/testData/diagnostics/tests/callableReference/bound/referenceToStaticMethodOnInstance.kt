// FILE: A.java

public class A {
    public static void test() {}
}

// FILE: test.kt

enum class E { EN }

fun test() {
    A()::<!UNRESOLVED_REFERENCE!>test<!>
    E.EN::<!UNRESOLVED_REFERENCE!>valueOf<!>
}
