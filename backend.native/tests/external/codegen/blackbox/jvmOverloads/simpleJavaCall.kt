// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME
// FILE: Test.java

public class Test {
    public static String invokeMethodWithOverloads() {
        C c = new C();
        return c.foo();
    }
}

// FILE: simple.kt

class C {
    @kotlin.jvm.JvmOverloads public fun foo(o: String = "O", k: String = "K"): String = o + k
}

fun box(): String {
    return Test.invokeMethodWithOverloads()
}
