// TARGET_BACKEND: JVM

// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.IntroducedAt

class C {
    fun foo(
        a : Int = 1,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("2") c: Int = 3,
        f : (Int) -> Int
    ) = f(a + b + c)

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun mid(
        a : Int = 1,
        @IntroducedAt("2") a1: Int = 2,
        @IntroducedAt("1") b: Int = 3,
        f : (Int) -> Int
    ) = f(a + a1 + b)
}

fun test1() : String {
    val c = C()
    val l = { x: Int -> x + 1 }
    val m1 = C::class.java.getMethod("foo", Int::class.java, kotlin.jvm.functions.Function1::class.java)
    val m2 = C::class.java.getMethod("foo", Int::class.java, Int::class.java, kotlin.jvm.functions.Function1::class.java)

    val v1 = m1.invoke(c, 10, l) as Int
    val v2 = m2.invoke(c, 10, 2, l) as Int
    val v3 = c.foo(10, 2, 3, l)

    return if ((v1 == v2) && (v1 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val c = C()
    val l = { x: Int -> x + 1 }
    val m1 = C::class.java.getMethod("mid", Int::class.java, kotlin.jvm.functions.Function1::class.java)
    val m2 = C::class.java.getMethod("mid", Int::class.java, Int::class.java, kotlin.jvm.functions.Function1::class.java)

    val v1 = m1.invoke(c, 10, l) as Int
    val v2 = m2.invoke(c, 10, 3, l) as Int
    val v3 = c.mid(10, 2, 3, l)

    return if ((v1 == v2) && (v1 == v3)) "K" else " Err2: $v1 $v2 $v3"
}

fun box() = test1() + test2()
