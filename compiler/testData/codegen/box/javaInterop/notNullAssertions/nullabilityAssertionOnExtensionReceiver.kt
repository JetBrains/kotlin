// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt

import kotlin.test.*

fun String.extension() {}

fun box(): String {
    assertFailsWith<NullPointerException> { J.s().extension() }
    return "OK"
}

// FILE: J.java
public class J {
    public static String s() { return null; }
}
