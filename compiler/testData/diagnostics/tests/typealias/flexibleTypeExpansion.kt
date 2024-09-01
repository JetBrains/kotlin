// FIR_IDENTICAL
// FILE: J.java
public class J {
    public static <T> T id(T t) { return null; }
    public static String bar() { return null; }
}

// FILE: main.kt

typealias S = String?

fun foo(x1: S, y1: String?) {
    val x2 = J.id(x1) ?: return
    val y2 = J.id(y1) ?: return
    val t = J.bar() ?: return // StackOverflowError happeded when analysing this piece of code

    x2.length
    y2.length
    t.length
}
