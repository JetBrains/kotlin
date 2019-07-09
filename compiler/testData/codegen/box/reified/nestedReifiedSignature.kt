// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME

package test

import kotlin.test.assertEquals

open class A<T1, T2, T3>

inline fun <reified T, reified R> foo(): Array<A<*,*,*>> {
    val x = object {
        inline fun <reified T1, reified T> bar(): A<*,*,*> = object : A<T1,T,R>() {}
        fun f1() = bar<T, R>()
        fun f2() = bar<R, T>()
        fun f3() = bar<Boolean, T>()
        fun f4() = bar<T, Boolean>()
    }

    return arrayOf(x.f1(), x.f2(), x.f3(), x.f4())
}

fun box(): String {
    val result = foo<Double, Int>()

    val expected = arrayOf(
            Triple("java.lang.Double", "java.lang.Integer", "java.lang.Integer"),
            Triple("java.lang.Integer", "java.lang.Double", "java.lang.Integer"),
            Triple("java.lang.Boolean", "java.lang.Double", "java.lang.Integer"),
            Triple("java.lang.Double", "java.lang.Boolean", "java.lang.Integer")
    ).map { "test.A<${it.first}, ${it.second}, ${it.third}>" }

    for (i in expected.indices) {
        assertEquals(expected[i], result[i].javaClass.getGenericSuperclass()?.toString(), "$i-th element")
    }

    return "OK"
}
