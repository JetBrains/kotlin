// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    fun Int.foo(
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$this/$b/$c"

    suspend fun bar(
        a : Int = 1,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$this/$b/$c"

    @JvmName("javaName")
    fun kotlinName(
        a : Int = 1,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$this/$b/$c"
}

fun test1() : String {
    val c = C()
    val m1 = C::class.java.getMethod("foo", Int::class.java)
    val m2 = C::class.java.getMethod("foo", Int::class.java, String::class.java)

    val v1 = m1.invoke(c, 10) as String
    val v2 = m2.invoke(c, 10, "hello") as String
    val v3 = with(c) { 10.foo("hello", true) }

    return if ((v1 == v2) && (v1 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val c = C()
    val m1 = C::class.java.getMethod("javaName", Int::class.java)
    val m2 = C::class.java.getMethod("javaName", Int::class.java, String::class.java)

    val v1 = m1.invoke(c, 10) as String
    val v2 = m2.invoke(c, 10, "hello") as String
    val v3 = c.kotlinName(10, "hello", true)

    return if ((v1 == v2) && (v1 == v3)) "K" else "Err1: $v1 $v2 $v3 "
}

fun box() = test1() + test2()