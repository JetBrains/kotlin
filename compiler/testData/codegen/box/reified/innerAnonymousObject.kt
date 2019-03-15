// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
package test

import kotlin.test.assertEquals

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

fun box(): String {
    val y = foo<String>().bar();
    assertEquals("OK", y.toString())
    assertEquals("test.A<java.lang.String>", y.javaClass.getGenericSuperclass()?.toString())
    return "OK"
}
