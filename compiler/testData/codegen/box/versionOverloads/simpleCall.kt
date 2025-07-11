// TARGET_BACKEND: JVM

// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.experimental.IntroducedAt

class C {
    fun foo(
        a : Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("2") c: Int = 3,
    ) = a + b + c

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun mid(
        a : Int = 1,
        @IntroducedAt("2") a1: Int = 2,
        @IntroducedAt("1") b: Int = 3,
    ) = a + a1 + b
}

fun test1() : String {
    val c = C()
    val m1 = C::class.java.getMethod("foo", Int::class.java)
    val m2 = C::class.java.getMethod("foo", Int::class.java, Int::class.java)

    val v1 = m1.invoke(c, 10) as Int
    val v2 = m2.invoke(c, 10, 2) as Int
    val v3 = c.foo(10, 2, 3)

    return if ((v1 == v2) && (v1 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val c = C()
    val m1 = C::class.java.getMethod("mid", Int::class.java)
    val m2 = C::class.java.getMethod("mid", Int::class.java, Int::class.java)

    val v1 = m1.invoke(c, 10) as Int
    val v2 = m2.invoke(c, 10, 3) as Int
    val v3 = c.mid(10, 2, 3)

    return if ((v1 == v2) && (v1 == v3)) "K" else " Err2: $v1 $v2 $v3"
}

fun box() = test1() + test2()
