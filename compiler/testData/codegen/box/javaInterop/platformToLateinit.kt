// TARGET_BACKEND: JVM_IR
// ISSUE: KT-75649

// FILE: J.java
public interface J {
    Object foo();
}
// FILE: 1.kt
class C(j: J) {
    private lateinit var x: Any

    init {
        x = j.foo()
    }
}

fun box(): String {
    try {
        C(object : J { override fun foo() = null })
    } catch (e: NullPointerException) {
        // expected
        return "OK"
    }
    return "FAIL: didn't throw."
}
