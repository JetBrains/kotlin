// TARGET_BACKEND: JVM

// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.IntroducedAt
import kotlin.jvm.JvmOverloads

class C {
    @Suppress("CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
    @JvmOverloads
    fun foo(
        a: Int = 1,
        @IntroducedAt("3") a1: Int = 2,
        @IntroducedAt("2") b : Int = 3,
    ) = a + a1 + b
}


fun box() : String {
    val c = C()
    val foo0 = C::class.java.getMethod("foo")
    val foo1 = C::class.java.getMethod("foo", Int::class.java)
    val foo2 = C::class.java.getMethod("foo", Int::class.java, Int::class.java)

    val v0 = foo0.invoke(c) as Int
    val v1 = foo1.invoke(c, 1) as Int
    val v2 = foo2.invoke(c, 1, 3) as Int
    val v3 = c.foo(1, 2, 3)

    return if ((v0 == v1) && (v1 == v2) && (v1 == v3)) "OK" else " Err2: $v0 $v1 $v2 $v3"
}
