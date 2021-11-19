// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: V
// Caused by: java.util.NoSuchElementException: Sequence contains no element matching the predicate.
// at org.jetbrains.kotlin.backend.jvm.lower.MappedEnumWhenLowering.visitClassNew(MappedEnumWhenLowering.kt:232)
// FILE: E.java
public enum E {
    A();
    public static void values(boolean b) {
    }
}

// FILE: test.kt

fun f(e: E) = when (e) {
    E.A -> "OK"
}

fun box(): String {
    return f(E.A)
}
