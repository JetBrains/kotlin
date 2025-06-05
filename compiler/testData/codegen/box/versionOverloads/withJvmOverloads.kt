// TARGET_BACKEND: JVM
// CHECK_BYTECODE_LISTING
// WITH_STDLIB
@file:OptIn(ExperimentalStdlibApi::class)

import kotlin.jvm.JvmOverloads

class C {
    @Suppress("CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
    @JvmOverloads
    fun foo(
        a: Int = 1,
        @IntroducedAt("3") a1: String = "hello",
        @IntroducedAt("2") b : Boolean = true,
    ) = "$a/$a1/$b"
}

class BiggerC {
    @Suppress("CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION")
    @JvmOverloads
    fun foo(
        a : Int,
        @IntroducedAt("1") B: String = "",
        @IntroducedAt("1") b1: String = "",
        @IntroducedAt("2") c: Float = 0f,
        @IntroducedAt("3") d: Int = 0,
    ) {}

}

@Suppress("CONFLICT_WITH_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
@JvmOverloads
fun bar(
    a: Int = 1,
    @IntroducedAt("3") a1: Int = 2,
    @IntroducedAt("2") b : Int = 3,
    @IntroducedAt("4") c: String = ""
) {}

fun box() : String {
    val c = C()
    val foo0 = C::class.java.getMethod("foo")
    val foo1 = C::class.java.getMethod("foo", Int::class.java)
    val foo2 = C::class.java.getMethod("foo", Int::class.java, Boolean::class.java)

    val v0 = foo0.invoke(c) as String
    val v1 = foo1.invoke(c, 1) as String
    val v2 = foo2.invoke(c, 1, true) as String
    val v3 = c.foo(1, "hello", true)

    return if ((v0 == v1) && (v1 == v2) && (v1 == v3)) "OK" else " Err2: $v0 $v1 $v2 $v3"
}
