// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// DUMP_EXTERNAL_CLASS: J
// FILE: J.java

public class J {
    public void bar() {}
}

// FILE: javaMethod.kt

fun test(j: J) = j.bar()
