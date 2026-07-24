// TARGET_BACKEND: JVM
// WITH_REFLECT
// FILE: J.java
public class J extends A<String> {}

// FILE: box.kt
import kotlin.test.assertEquals

open class A<T> {
    fun f(): T = null!!
    var p: T
        get() = null!!
        set(value) {}
}

fun box(): String {
    assertEquals("fun J.f(): kotlin.String!", J::f.toString())
    assertEquals("var J.p: kotlin.String!", J::p.toString())
    return "OK"
}
