// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt

import kotlin.test.*

var component1Evaluated = false

// NB extension receiver is nullable
operator fun J?.component1() = 1.also { component1Evaluated = true }

private operator fun J.component2() = 2

fun use(x: Any) {}

fun box(): String {
    assertFailsWith<IllegalArgumentException> {
        val (a, b) = J.j()
    }
    if (!component1Evaluated) return "component1 should be evaluated"
    return "OK"
}


// FILE: J.java
public class J {
    public static J j() { return null; }
}
