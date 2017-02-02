// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

import kotlin.test.assertEquals

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

fun box(): String {
    val y = foo<String>();
    assertEquals("java.lang.String", y.f())
    return "OK"
}
