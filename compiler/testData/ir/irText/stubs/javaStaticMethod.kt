// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// DUMP_EXTERNAL_CLASS: J
// FILE: J.java

public class J {
    public static void bar() {}
}

// FILE: javaStaticMethod.kt

fun test() = J.bar()
