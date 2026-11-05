// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: J.java
public class J {
    public static <T> T id(T t) { return t; }
}

// FILE: box.kt
inline fun <reified T> isT(x: T, y: Any?): Boolean = y is T
inline fun <reified T> asT(x: T, y: Any?): T = y as T

inline fun <reified R> outerIs(r: R, y: Any?): Boolean = isT(J.id(r), y)
inline fun <reified R> outerAs(r: R, y: Any?): R = asT(J.id(r), y)

fun box(): String {
    if (outerIs("", null)) return "Fail 1"
    if (!outerIs("", "x")) return "Fail 2"
    try {
        outerAs("", null)
        return "Fail 3"
    } catch (e: Exception) {
        return "OK"
    }
}
