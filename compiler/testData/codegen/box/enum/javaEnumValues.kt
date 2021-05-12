// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
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
