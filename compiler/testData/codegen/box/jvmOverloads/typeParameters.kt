// TARGET_BACKEND: JVM

// WITH_STDLIB

// FILE: box.kt
class C {
    @kotlin.jvm.JvmOverloads public fun <X> foo(x: X, s: String = "OK"): String {
        return s
    }
}

fun box(): String {
    return A().test()
}

// FILE: A.java

public class A {
    public String test() {
        return new C().foo(42);
    }
}

