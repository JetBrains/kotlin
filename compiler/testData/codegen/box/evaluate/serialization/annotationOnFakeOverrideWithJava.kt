// ISSUE: KT-72356
// TARGET_BACKEND: JVM
// FULL_JDK
// FILE: A.kt
annotation class A(val x: String)

annotation class Something

// FILE: J.java
public class J { @A(x = "12345678") public int a = 0; }

// FILE: D.kt
class D {               @Something fun bar() {} }

class E : J()

fun box(): String {
    return "OK"
}
