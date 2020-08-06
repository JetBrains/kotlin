// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// WITH_RUNTIME
// FULL_JDK
// FILE: A.java
public class A {
    public static A nil() { return null; }
}

// FILE: test.kt
fun box(): String {
    // There is a null check on both arguments of Map.getOrDefault, so we expect this code to throw an exception.
    // Which exception this is depends on the language version (it's a NullPointerException in Kotlin 1.4).
    val m = mapOf<A, A>()
    try {
        m.getOrDefault(A.nil(), A())
    } catch (e: Exception) {
        try {
            m.getOrDefault(A(), A.nil())
        } catch (e: Exception) {
            return "OK"
        }
        return "Fail 2"
    }
    return "Fail 1"
}
