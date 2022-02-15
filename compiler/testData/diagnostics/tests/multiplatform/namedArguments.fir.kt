// MODULE: m1-common
// FILE: common.kt

expect class Foo(zzz: Int) {
    <!PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED!>constructor(aaa: Boolean)<!>

    fun f1(xxx: String): String
}

expect fun f2(xxx: Int)

fun testCommon() {
    Foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>zzz<!> = 0)
    val f = Foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>aaa<!> = true)
    f.f1(<!NAMED_ARGUMENTS_NOT_ALLOWED!>xxx<!> = "")
    f2(<!NAMED_ARGUMENTS_NOT_ALLOWED!>xxx<!> = 42)
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
