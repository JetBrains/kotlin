// TARGET_BACKEND: JVM
// FILE: A.java
public class A {
    public static String f() {
        return "O";
    }
}
// FILE: a.kt
open class C(x: String, y: String) {
    val result = x + y
}

fun box() = object : C(y = "K", x = A.f()) {}.result
