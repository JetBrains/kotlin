// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: safeCallWithElvisAndEnhancedNullability.kt

import kotlin.test.assertEquals

fun check(a : A?) : Int {
    return a?.y?.x ?: (a?.x ?: 3)
}

fun checkLeftAssoc(a : A?) : Int {
    return (a?.y?.x ?: a?.x) ?: 3
}

fun box() : String {
    val a1 = A(2, A(1, null))
    val a2 = A(2, null)
    val a3 = null

    assertEquals(1, check(a1))
    assertEquals(2, check(a2))
    assertEquals(3, check(a3))

    assertEquals(1, checkLeftAssoc(a1))
    assertEquals(2, checkLeftAssoc(a2))
    assertEquals(3, checkLeftAssoc(a3))

    return "OK"
}

// FILE: A.java
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class A {
    private Integer x;
    private A y;

    public A(Integer x, A y) {
        this.x = x;
        this.y = y;
    }

    @NotNull
    public Integer getX() { return x; }

    @Nullable
    public A getY() { return y; }
}
