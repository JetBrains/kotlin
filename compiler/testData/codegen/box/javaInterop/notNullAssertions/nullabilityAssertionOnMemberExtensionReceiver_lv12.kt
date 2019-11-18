// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt

import kotlin.test.*

class C {
    fun test() { J.s().memberExtension() }
    fun String.memberExtension() {}
}

fun box(): String {
    assertFailsWith<IllegalStateException> { C().test() }
    return "OK"
}

// FILE: J.java
public class J {
    public static String s() { return null; }
}
