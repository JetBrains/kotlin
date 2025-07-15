// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: lib.kt

abstract class A {
    abstract fun f(): String
}

inline fun<reified T : Any> foo(): A {
    return object : A() {
        override fun f(): String {
            return T::class.java.getName()
        }
    }
}

// FILE: main.kt
import kotlin.test.assertEquals

fun box(): String {
    val y = foo<String>();
    assertEquals("java.lang.String", y.f())
    return "OK"
}
