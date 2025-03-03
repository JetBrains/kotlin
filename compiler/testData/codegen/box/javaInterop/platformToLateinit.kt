// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
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

    fun bar() = x
}

fun box(): String {
    C(object : J { override fun foo() = null })
    return "OK"
}
