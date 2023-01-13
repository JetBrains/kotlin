// TARGET_BACKEND: JVM_IR
// ISSUE: KT-53441
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

fun box() = J.OK[0] // accessible by JVM rules as J.OK, but not I.OK
