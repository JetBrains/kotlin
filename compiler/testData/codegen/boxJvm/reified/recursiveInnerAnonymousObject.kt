// TARGET_BACKEND: JVM

// WITH_STDLIB
package test

import kotlin.test.assertEquals

abstract class A<R> {
    abstract fun f(): String
    override fun toString() = f()
}

abstract class G {
    abstract fun bar(): Any
}

inline fun<reified T> baz(): G {
    return object : G() {
        override fun bar(): Any {
            return object : A<T>() {
                override fun f(): String = "OK"
            }
        }
    }
}

inline fun<T1, T2, T3, T4, T5, T6, reified R1, reified R2> foo(): Pair<G, G> {
    return Pair(baz<R1>(), baz<R2>())
}

fun box(): String {
    val res = foo<Int, Int, Int, Int, Int, Int, Int, String>();
    val x1 = res.first.bar()
    val x2 = res.second.bar()
    assertEquals("OK", x1.toString())
    assertEquals("OK", x2.toString())
    assertEquals("test.A<java.lang.Integer>", x1.javaClass.getGenericSuperclass()?.toString())
    assertEquals("test.A<java.lang.String>", x2.javaClass.getGenericSuperclass()?.toString())
    return "OK"
}
