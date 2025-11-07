// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

class C {
    companion object {
        @JvmStatic fun foo(
            a: Int = 1,
            @IntroducedAt("1") b: String = "hello",
            @IntroducedAt("2") c: Boolean = true,
        ) = "$a/$b/$c"
    }
}

object D {
    @JvmStatic fun foo(
        a: Int = 1,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("2") c: Boolean = true,
    ) = "$a/$b/$c"
}

fun test1() : String {
    val m1 = C::class.java.getMethod("foo", Int::class.java)
    val m2 = C::class.java.getMethod("foo", Int::class.java, String::class.java)

    val v1 = m1.invoke(null, 10) as String
    val v2 = m2.invoke(null, 10, "hello") as String
    val v3 = C.foo(10, "hello", true)

    return if ((v1 == v2) && (v1 == v3)) "O" else "Err1: $v1 $v2 $v3 "
}

fun test2() : String {
    val m1 = D::class.java.getMethod("foo", Int::class.java)
    val m2 = D::class.java.getMethod("foo", Int::class.java, String::class.java)

    val v1 = m1.invoke(null, 10) as String
    val v2 = m2.invoke(null, 10, "hello") as String
    val v3 = D.foo(10, "hello", true)

    return if ((v1 == v2) && (v1 == v3)) "K" else " Err2: $v1 $v2 $v3"
}

fun box() = test1() + test2()
