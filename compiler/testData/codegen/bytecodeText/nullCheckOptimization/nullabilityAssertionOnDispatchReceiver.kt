// TARGET_BACKEND: JVM
// WITH_STDLIB
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

// @TestKt.class:
// 0 checkNotNullExpressionValue
// 0 checkExpressionValueIsNotNull
