// FIR_IDENTICAL
// TARGET_BACKEND: JVM
// ISSUE: KT-70753

// FILE: Lib.java
public class Lib {
    public static A getA() {
        return null;
    }
}
// FILE: main.kt
class A(val string: String)

fun box(): String {
    val a: A = try {
        Lib.getA()
    } catch (e: Exception) {
        A("OK")
    }
    return "OK"
}