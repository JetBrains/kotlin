// MODULE: m1-common
// FILE: common.kt

expect class Foo(zzz: Int) {
    constructor(aaa: Boolean)

    fun f1(xxx: String): String
}

expect fun f2(xxx: Int)

fun testCommon() {
    Foo(zzz = 0)
    val f = Foo(aaa = true)
    f.f1(xxx = "")
    f2(xxx = 42)
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class Foo actual constructor(val aaa: Boolean) {
    actual constructor(zzz: Int) : this(zzz == 0)

    actual fun f1(xxx: String) = xxx
}

actual fun f2(xxx: Int) {}

fun testPlatform() {
    Foo(zzz = 0)
    val f = Foo(aaa = true)
    f.f1(xxx = "")
    f2(xxx = 42)
}
