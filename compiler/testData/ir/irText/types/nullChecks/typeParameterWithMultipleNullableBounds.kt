// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// FILE: typeParameterWithMultipleNullableBounds.kt
fun <T> f(x: T): Int? where T : CharSequence?, T : Comparable<T>? {
    return x?.compareTo(x)
}

fun test() {
    f(J.s())
    f(J.STRING)
}


// FILE: J.java
public class J {
    public static String STRING = s()
    public static String s() { return null; }
}
