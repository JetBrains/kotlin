// TARGET_BACKEND: JVM
// FILE: typeParameterWithMultipleNullableBounds.kt
fun <T> f(x: T): Int? where T : CharSequence?, T : Comparable<T>? {
    return x?.compareTo(x)
}

fun box() = f(J.s()) ?: "OK"

// FILE: J.java
public class J {
    public static String s() { return null; }
}
