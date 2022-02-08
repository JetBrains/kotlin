// FIR_IDENTICAL
// FILE: A.java
import org.checkerframework.checker.nullness.qual.*;
public class A {
    @NonNull
    public Object foo() { return null; }
}
// FILE: B.java
public class B {
    public static void assertNonNull(Object x) {}
}

// FILE: main.kt
fun main() {
    A()
    B.assertNonNull(null)
}
