// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: A.java
public class A {
    public static A nil() { return null; }
}

// FILE: test.kt
fun box(): String {
    val l = listOf<A>()
    return if (l.indexOf(A.nil()) == -1) "OK" else "Fail"
}
