// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class, ExperimentalStdlibApi::class)

@JvmInline value class PositiveInt(val n: Int = 0)

class C {
    @JvmExposeBoxed
    fun takesPositiveInt(
        a: String,
        @IntroducedAt("1") b: PositiveInt = PositiveInt(1),
        @IntroducedAt("2") c: Boolean = true
    ) = "$a/$b/$c"

    @JvmExposeBoxed
    fun returnsPositiveInt(
        a: String,
        @IntroducedAt("1") b: Int = 2,
        @IntroducedAt("2") c: Boolean = true
    ): PositiveInt = PositiveInt(3)
}

fun test1() : String {
    val c = C()

    val m1 = C::class.java.getMethod("takesPositiveInt", String::class.java)
    val m2 = C::class.java.getMethod("takesPositiveInt", String::class.java, PositiveInt::class.java)

    val v1 = m1.invoke(c, "hello") as String
    val v2 = m2.invoke(c, "hello", PositiveInt(1)) as String
    val v3 = c.takesPositiveInt("hello", PositiveInt(1), true)

    return if ((v1 == v2) && (v1 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val c = C()

    val m1 = C::class.java.getMethod("returnsPositiveInt", String::class.java)
    val m2 = C::class.java.getMethod("returnsPositiveInt", String::class.java, Int::class.java)

    val v1 = m1.invoke(c, "hello") as PositiveInt
    val v2 = m2.invoke(c, "hello", 2) as PositiveInt
    val v3 = c.returnsPositiveInt("hello", 2, true)

    return if ((v1 == v2) && (v1 == v3)) "K" else "Err1: $v1 $v2 $v3 "
}

fun box() = test1() + test2()
