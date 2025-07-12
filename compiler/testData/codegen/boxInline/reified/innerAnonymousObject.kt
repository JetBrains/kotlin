// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: lib.kt
package test

abstract class A<R> {
    abstract fun f(): String
    override fun toString() = f()
}

abstract class G {
    abstract fun bar(): Any
}

inline fun<reified T> foo(): G {
    return object : G() {
        override fun bar(): Any {
            return object : A<T>() {
                 override fun f(): String = "OK"
            }
        }
    }
}

// FILE: main.kt
package test
import kotlin.test.assertEquals

fun box(): String {
    val y = foo<String>().bar();
    assertEquals("OK", y.toString())
    assertEquals("test.A<java.lang.String>", y.javaClass.getGenericSuperclass()?.toString())
    return "OK"
}
