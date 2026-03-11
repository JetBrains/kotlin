// TARGET_BACKEND: JVM
// FILE: typeParameterWithMultipleNotNullableBounds.kt
fun <T> f(x: T): Int where T : CharSequence, T : Comparable<T> {
    try {
        return x.compareTo(x)
    } catch (e: NullPointerException) {
        return 42
    }
}

fun box() = try {
    val r = f(J.s())
    if (r == 42) "FAIL" else "Unexpected, x.compareTo(x) should have NPE'd"
} catch (e: NullPointerException) {
    "OK"
}


// FILE: J.java
public class J {
    public static String s() { return null; }
}
