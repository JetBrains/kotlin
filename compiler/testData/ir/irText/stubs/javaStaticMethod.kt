// !DUMP_DEPENDENCIES
// FILE: J.java

public class J {
    public static void bar() {}
}

// FILE: javaStaticMethod.kt
// FIR_IDENTICAL

fun test() = J.bar()