// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K1: JVM_IR
// CHECK_BYTECODE_LISTING
// WITH_STDLIB


@file:OptIn(ExperimentalVersionOverloading::class, ExperimentalStdlibApi::class)

class ExposedC {
    @Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
    @JvmExposeBoxed("foo_exposed")
    @JvmOverloads
    fun foo(
        a: Float = 1f,
        @IntroducedAt("3") a1: UInt = 3u,
        @IntroducedAt("2") b : Boolean = true,
    ) = "$a/$a1/$b"
}

class EverythingC {
    @Suppress("CONFLICT_VERSION_AND_JVM_OVERLOADS_ANNOTATION", "NON_ASCENDING_VERSION_ANNOTATION")
    @JvmExposeBoxed("foo_exposed")
    @JvmName("foo_renamed")
    @JvmOverloads
    fun foo(
        a: Float = 1f,
        @IntroducedAt("3") a1: UInt = 3u,
        @IntroducedAt("2") b : Boolean = true,
    ) = "$a/$a1/$b"

}

fun test1(): String {
    val c = ExposedC()
    val foo0 = ExposedC::class.java.getMethod("foo_exposed")
    val foo1 = ExposedC::class.java.getMethod("foo_exposed", Float::class.java)
    val foo2 = ExposedC::class.java.getMethod("foo_exposed", Float::class.java, Boolean::class.java)
    val foo2a = ExposedC::class.java.getMethod("foo_exposed", Float::class.java, UInt::class.java)
    val foo3a = ExposedC::class.java.getMethod("foo_exposed", Float::class.java, UInt::class.java, Boolean::class.java)

    val v0 = foo0.invoke(c) as String
    val v1 = foo1.invoke(c, 1f) as String
    val v2 = foo2.invoke(c, 1f, true) as String
    val v2a = foo2a.invoke(c, 1f, 3u) as String
    val v3a = foo3a.invoke(c,1f, 3u, true)

    val v3 = c.foo(1f, 3u, true)

    return if ((v0 == v1) && (v1 == v2) && (v2 == v2a) && (v2a == v3)&& (v3 == v3a)) "O" else " Err2: $v0 $v1 $v2 $v2a $v3 $v3a"
}

fun test2(): String {
    val c = EverythingC()
    val foo0 = EverythingC::class.java.getMethod("foo_renamed")
    val foo1 = EverythingC::class.java.getMethod("foo_renamed", Float::class.java)
    val foo2 = EverythingC::class.java.getMethod("foo_renamed", Float::class.java, Boolean::class.java)
    val foo2a = EverythingC::class.java.getMethod("foo_renamed", Float::class.java, Int::class.java)
    val foo2e = EverythingC::class.java.getMethod("foo_exposed", Float::class.java, UInt::class.java)
    val foo3a = EverythingC::class.java.getMethod("foo_renamed", Float::class.java, Int::class.java, Boolean::class.java)
    val foo3e = EverythingC::class.java.getMethod("foo_exposed", Float::class.java, UInt::class.java, Boolean::class.java)

    val v0 = foo0.invoke(c) as String
    val v1 = foo1.invoke(c, 1f) as String
    val v2 = foo2.invoke(c, 1f, true) as String
    val v2a = foo2a.invoke(c, 1f, 3) as String
    val v2e = foo2e.invoke(c, 1f, 3u) as String
    val v3a = foo3a.invoke(c, 1f, 3, true)
    val v3e = foo3e.invoke(c, 1f, 3u, true)
    val v3 = c.foo(1f, 3u, true)

    return if ((v0 == v1) && (v1 == v2) && (v2 == v2a) && (v2a == v2e) && (v2e == v3) && (v3 == v3a) && (v3 == v3e)) "K" else " Err2: $v0 $v1 $v2 $v2a $v2e $v3 $v3a $v3e"
}

fun box() : String {
    return test1() + test2()
}