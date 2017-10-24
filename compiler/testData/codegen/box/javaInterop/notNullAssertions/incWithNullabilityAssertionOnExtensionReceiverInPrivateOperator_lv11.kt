// TARGET_BACKEND: JVM
// FILE: test.kt
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.1
private operator fun A.inc() = A()

fun box(): String {
    var aNull = A.n()
    aNull++
    // NB no exception is thrown in language version 1.1
    return "OK"
}

// FILE: A.java
public class A {
    public static A n() { return null; }
}