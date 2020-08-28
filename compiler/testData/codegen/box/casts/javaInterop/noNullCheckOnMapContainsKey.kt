// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: A.java
public class A {
    public static A nil() { return null; }
}

// FILE: test.kt
fun box(): String {
    val m = mapOf<A, String>()
    return if (m.containsKey(A.nil())) "Fail" else "OK"
}
