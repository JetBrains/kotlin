// !WITH_NEW_INFERENCE
// FILE: test/J.java

package test;

public class J {
    public J(String s, Runnable r, Boolean z) {
    }
}

// FILE: usage.kt

package test

fun test() {
    <!INAPPLICABLE_CANDIDATE!>J<!>("", r = { }, z = false)
}
