// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt

import kotlin.test.*

fun String.extension() {}

fun box(): String {
    assertFailsWith<IllegalStateException> { J.s().extension() }
    return "OK"
}

// FILE: J.java
public class J {
    public static String s() { return null; }
}
