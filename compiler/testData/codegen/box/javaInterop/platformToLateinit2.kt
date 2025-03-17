// TARGET_BACKEND: JVM_IR
// ISSUE: KT-75649

// FILE: J.java
public interface J {
    Object foo();
}
// FILE: 1.kt
class C(val j: J) {
    private lateinit var x: Any

    fun init() {
        x = j.foo()
    }
}

fun box(): String {
    val c = C(object : J { override fun foo() = null })
    try {
        c.init()
    } catch (e: NullPointerException) {
        return "OK"
    }
    return "FAIL: didn't throw"
}
