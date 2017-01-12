// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_RUNTIME

import kotlin.test.assertEquals

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

fun box(): String {
    val y = foo<String>();
    assertEquals("OK", y.f())
    assertEquals("A<java.lang.String>", y.javaClass.getGenericSuperclass()?.toString())
    return "OK"
}
