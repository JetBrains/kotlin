// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
// LANGUAGE_VERSION: 1.2
import kotlin.test.*

class C {
    fun test() { J.s().memberExtension() }
    private fun String.memberExtension() {}
}

fun box(): String {
    assertFailsWith<IllegalStateException> {
        C().test()
    }
    return "OK"
}

// FILE: J.java
public class J {
    public static String s() { return null; }
}