// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    fun foo(
        a : Int = 1,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
        f : (String) -> String
    ) = f("$a/$b/$c")

    @Suppress("NON_ASCENDING_VERSION_ANNOTATION")
    fun mid(
        a : Int = 1,
        @IntroducedAt("2") a1: String = "hello",
        @IntroducedAt("1") b: Boolean = true,
        f : (String) -> String
    ) = f("$a/$a1/$b")
}

fun test1() : String {
    val c = C()
    val l: (String) -> String = String::uppercase
    val m1 = C::class.java.getMethod("foo", Int::class.java, kotlin.jvm.functions.Function1::class.java)
    val m2 = C::class.java.getMethod("foo", Int::class.java, String::class.java, kotlin.jvm.functions.Function1::class.java)

    val v1 = m1.invoke(c, 10, l) as String
    val v2 = m2.invoke(c, 10, "hello", l) as String
    val v3 = c.foo(10, "hello", true, l)

    return if ((v1 == v2) && (v1 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val c = C()
    val l: (String) -> String = String::uppercase
    val m1 = C::class.java.getMethod("mid", Int::class.java, kotlin.jvm.functions.Function1::class.java)
    val m2 = C::class.java.getMethod("mid", Int::class.java, Boolean::class.java, kotlin.jvm.functions.Function1::class.java)

    val v1 = m1.invoke(c, 10, l) as String
    val v2 = m2.invoke(c, 10, true, l) as String
    val v3 = c.mid(10, "hello", true, l)

    return if ((v1 == v2) && (v1 == v3)) "K" else " Err2: $v1 $v2 $v3"
}

fun box() = test1() + test2()
