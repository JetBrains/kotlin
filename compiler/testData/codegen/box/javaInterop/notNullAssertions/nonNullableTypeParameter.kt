// TARGET_BACKEND: JVM
// FILE: a.kt
interface I {
    fun <T : String> f(x: T) = x
}

class C : I

fun box() = try {
    B.f()
    "FAIL"
} catch (e: NullPointerException) {
    "OK"
}

// FILE: B.java
public class B {
    public static String f() {
        return new C().<String>f(null);
    }
}
