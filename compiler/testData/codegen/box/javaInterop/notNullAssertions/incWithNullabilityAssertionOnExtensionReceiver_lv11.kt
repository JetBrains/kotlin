// TARGET_BACKEND: JVM
// FILE: test.kt
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.1
import kotlin.test.*

operator fun A.inc() = A()

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        var aNull = A.n()
        aNull++
    }

    return "OK"
}

// FILE: A.java
public class A {
    public static A n() { return null; }
}