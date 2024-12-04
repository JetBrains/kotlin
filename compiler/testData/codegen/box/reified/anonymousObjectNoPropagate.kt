// TARGET_BACKEND: JVM
// WITH_STDLIB

import kotlin.test.assertEquals

interface A {
    fun f1(): String
    fun f2(): String
    fun f3(): String
}

fun doWork(block: () -> String) = block()
inline fun doWorkInline(block: () -> String) = block()

fun box(): String {
    val x = object {
        inline fun <reified T : Any> bar1(): A = object : A {
            override fun f1(): String = T::class.java.getName()
            override fun f2(): String = doWork { T::class.java.getName() }
            override fun f3(): String = doWorkInline { T::class.java.getName() }
        }

        inline fun <reified T : Any> bar2() = T::class.java.getName()
        inline fun <reified T : Any> bar3() = doWork { T::class.java.getName() }
        inline fun <reified T : Any> bar4() = doWorkInline { T::class.java.getName() }
    }

    val y: A = x.bar1<String>()
    assertEquals("java.lang.String", y.f1())
    assertEquals("java.lang.String", y.f2())
    assertEquals("java.lang.String", y.f3())


    assertEquals("java.lang.Integer", x.bar2<Int>())
    assertEquals("java.lang.Double", x.bar3<Double>())
    assertEquals("java.lang.Long", x.bar4<Long>())

    return "OK"
}
