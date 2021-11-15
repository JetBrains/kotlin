// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: A.java
public class A {
    public static A nil() { return null; }
}

// FILE: test.kt
fun box(): String {
    val m = mutableMapOf<A, String>()
    return if (m.remove(A.nil()) == null) "OK" else "Fail"
}
