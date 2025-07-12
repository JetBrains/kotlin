// TARGET_BACKEND: JVM

// WITH_STDLIB

// FILE: lib.kt
package test

abstract class A<R> {
    abstract fun f(): String
}

inline fun<reified T> foo(): A<T> {
    return object : A<T>() {
        override fun f(): String {
            return "OK"
        }
    }
}

// FILE: main.kt
package test
import kotlin.test.assertEquals

fun box(): String {
    val y = foo<String>();
    assertEquals("OK", y.f())
    assertEquals("test.A<java.lang.String>", y.javaClass.getGenericSuperclass()?.toString())
    return "OK"
}
