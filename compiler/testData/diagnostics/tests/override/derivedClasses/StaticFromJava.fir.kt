// MODULE: lib
// FILE: test/J.java
package test;

interface I {
    // Not a String to avoid constant inlining
    public static String[] OK = new String[]{"OK"};
}

public class J implements I {}

// MODULE: main(lib)
// FILE: k.kt
import test.J

fun test() {
    <!DEBUG_INFO_CALLABLE_OWNER("test.I.OK in test.I")!>J.OK<!>
}