// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

import kotlin.jvm.JvmOverloads

class C {
    @Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
    @JvmOverloads
    fun foo(
        a: Int = 1,
        @IntroducedAt("3") a1: String = "hello",
        @IntroducedAt("2") b : Boolean = true,
    ) = "$a/$a1/$b"
}

class BiggerC {
    @Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION")
    @JvmOverloads
    fun foo(
        a : Int,
        @IntroducedAt("1") b: String = "hello",
        @IntroducedAt("1") b1: String = "bye",
        @IntroducedAt("2") c: Float = 0f,
        @IntroducedAt("3") d: Int = 2,
    ) = "$a/$b/$b1/$c/$d"

}

@Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
@JvmOverloads
fun bar(
    a: Int = 1,
    @IntroducedAt("3") a1: Int = 2,
    @IntroducedAt("2") b : Int = 3,
    @IntroducedAt("4") c: String = ""
) {}

class ConflictingOverloads {
    @JvmOverloads @Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION")
    fun f(s: String, @IntroducedAt("1") x: Int = 0, @IntroducedAt("1") y: Long = 0L) {}
    fun f(a: Any, b: Boolean) {}
}

fun test1(): String {
    val c = C()
    val foo0 = C::class.java.getMethod("foo")
    val foo1 = C::class.java.getMethod("foo", Int::class.java)
    val foo2 = C::class.java.getMethod("foo", Int::class.java, Boolean::class.java)

    val v0 = foo0.invoke(c) as String
    val v1 = foo1.invoke(c, 1) as String
    val v2 = foo2.invoke(c, 1, true) as String
    val v3 = c.foo(1, "hello", true)

    return if ((v0 == v1) && (v1 == v2) && (v1 == v3)) "O" else " Err2: $v0 $v1 $v2 $v3"
}

fun test2(): String {
    val c = BiggerC()
    val foo1 = BiggerC::class.java.getMethod("foo", Int::class.java)
    val foo2 = BiggerC::class.java.getMethod("foo", Int::class.java, String::class.java, String::class.java)
    val foo3 = BiggerC::class.java.getMethod("foo", Int::class.java, String::class.java, String::class.java, Float::class.java)

    val v1 = foo1.invoke(c, 1) as String
    val v2 = foo2.invoke(c, 1, "hello", "bye") as String
    val v3 = foo3.invoke(c, 1, "hello", "bye", 0f) as String
    val v4 = c.foo(1, "hello", "bye", 0f, 2)

    return if ((v1 == v4) && (v2 == v4) && (v3 == v4)) "K" else " Err2: $v1 $v2 $v3 $v4"
}

fun box() : String {
    return test1() + test2()
}
