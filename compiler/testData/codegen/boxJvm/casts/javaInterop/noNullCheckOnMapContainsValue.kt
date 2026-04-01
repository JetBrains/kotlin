// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: A.java
public class A {
    public static A nil() { return null; }
}

// FILE: test.kt
fun box(): String {
    val m = mapOf<String, A>()
    return if (m.containsValue(A.nil())) "Fail" else "OK"
}
