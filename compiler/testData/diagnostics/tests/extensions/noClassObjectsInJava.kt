// FIR_IDENTICAL
// FILE: A.java
public class A {
    public static void foo() {}
    public static class Nested {}
}

// FILE: B.kt
fun Any?.bar() = 42

fun f1() = A.<!UNRESOLVED_REFERENCE!>bar<!>()
fun f2() = A.Nested.<!UNRESOLVED_REFERENCE!>bar<!>()
