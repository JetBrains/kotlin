// FILE: A.java
import java.io.Closeable;

public class A {
    public static void foo(Runnable r) {
    }

    public static void foo(Closeable c) {
    }
}

// FILE: test.kt

fun main() {
    A.<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!> { "Hello!" }
    A.foo(Runnable { <!UNUSED_EXPRESSION!>"Hello!"<!> })
}