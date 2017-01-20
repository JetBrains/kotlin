// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

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
    assertEquals("A<java.lang.String>", y.javaClass.getGenericSuperclass()?.toString())
    return "OK"
}
