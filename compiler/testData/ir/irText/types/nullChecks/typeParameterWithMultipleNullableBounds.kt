// TARGET_BACKEND: JVM
// FILE: typeParameterWithMultipleNullableBounds.kt
fun <T> f(x: T): Int? where T : CharSequence?, T : Comparable<T>? {
    return x?.compareTo(x)
}
fun <T> f2(vararg x: T): Int? where T : CharSequence?, T : Comparable<T>? {
    return x[0]?.compareTo(x[0])
}

fun test() {
    f(J.s())
    f(J.STRING)
    f(J.s())
    f(J.STRING)
}


// FILE: J.java
public class J {
    public static String STRING = s();
    public static String s() { return null; }
}
