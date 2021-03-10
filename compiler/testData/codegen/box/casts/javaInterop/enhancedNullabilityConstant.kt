// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FILE: box.kt
fun id(x: Long) = x

fun box(): String {
    val x = X()
    val y = id(x.y ?: 0 * 1)
    return if (y == 0L) "OK" else "fail: $y"
}

// FILE: X.java
import org.jetbrains.annotations.Nullable;

public class X {
    @Nullable
    Long getY() { return null; }
}
