// TARGET_BACKEND: JVM
// IGNORE_INLINER: IR

// WITH_STDLIB

import kotlin.test.assertEquals

inline fun <reified T : Any, reified R : Any> foo(): Array<String> {
    val x = object {
        inline fun <reified T1 : Any, reified T : Any> bar(): Array<String> = arrayOf(
                T1::class.java.getName(), T::class.java.getName(), R::class.java.getName()
        )
        fun f1() = bar<T, R>()
        fun f2() = bar<R, T>()
        fun f3() = bar<Boolean, T>()
        fun f4() = bar<T, Boolean>()
    }

    val x1 = x.f1()
    val x2 = x.f2()
    val x3 = x.f3()
    val x4 = x.f4()

    return arrayOf(
            x1[0], x1[1], x1[2],
            x2[0], x2[1], x2[2],
            x3[0], x3[1], x3[2],
            x4[0], x4[1], x4[2]
    )
}

fun box(): String {
    val result = foo<Double, Int>()

    val expected = arrayOf(
            "java.lang.Double", "java.lang.Integer", "java.lang.Integer",
            "java.lang.Integer", "java.lang.Double", "java.lang.Integer",
            "java.lang.Boolean", "java.lang.Double", "java.lang.Integer",
            "java.lang.Double", "java.lang.Boolean", "java.lang.Integer"
    )

    for (i in expected.indices) {
        assertEquals(expected[i], result[i], "$i-th element")
    }

    return "OK"
}
