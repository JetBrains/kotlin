// TARGET_BACKEND: JVM
// WITH_STDLIB
// CURIOUS_ABOUT: id, makeHash, test

@file:OptIn(kotlin.ExperimentalStdlibApi::class)

@JvmInline
value class I(val value: Int)

@JvmInline
value class D(val value: Double)

@JvmInline
value class S(val value: String)

class C(val prompt: String) {
    fun <@JvmSpecialize T> makeHash(x: T) = "$prompt: ${x.hashCode()}"
}

fun <@JvmSpecialize T> id(x: T) = x

fun test() {
    id("abc")
    id(42)
    id(42L)
    id(42.0)
    id(I(42))
    id(D(42.0))
    id(S("abc"))
    C("hash is").makeHash(42)
}
