// FILE: test/J.java

package test;

public class J {
    public static void foo(String s, Runnable r, Boolean z) {
    }
}

// FILE: usage.kt

package test

fun test() {
    J.foo("", <!NAMED_ARGUMENTS_NOT_ALLOWED!>r<!> = { }, <!NAMED_ARGUMENTS_NOT_ALLOWED!>z<!> = false)
}
