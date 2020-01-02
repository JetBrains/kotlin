// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: test.kt
import kotlin.test.*

fun box(): String {
    assertFailsWith<NullPointerException> { J.j().method() }
    assertFailsWith<NullPointerException> { J.j().field }
    assertFailsWith<NullPointerException> { J.j().field = 42 }
    return "OK"
}

// FILE: J.java
public class J {
    public Object field;

    public void method() {}

    public static J j() { return null; }
}
