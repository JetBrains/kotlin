// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

import kotlin.test.*

private operator fun A.inc() = A()

fun box(): String {
    assertFailsWith<NullPointerException> {
        var aNull = A.n()
        aNull++
    }

    return "OK"
}

// FILE: A.java
public class A {
    public static A n() { return null; }
}
