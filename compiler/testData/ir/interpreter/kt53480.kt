// FILE: J.java
public class J {
    public static int f() { return 0; }
}

// FILE: Main.kt
enum class A {
    E;
    val x = J.f()
}

const val name = A.E.name