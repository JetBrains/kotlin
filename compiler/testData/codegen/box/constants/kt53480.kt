// TARGET_BACKEND: JVM_IR
// FILE: J.java
public class J {
    public static int f() { return 0; }
}

// FILE: Main.kt
enum class A {
    OK;
    val x = J.f()
}

fun box(): String {
    return A.OK.name
}