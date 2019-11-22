// FILE: implicitNotNullOnPlatformType.kt
fun f(s: String) {}

fun test() {
    f(J.s())
    f(J.STRING)
}

// FILE: J.java
public class J {
    public static String STRING = s();
    public static String s() { return null; }
}
