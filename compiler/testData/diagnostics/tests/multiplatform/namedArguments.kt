// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class Foo(zzz: Int) {
    constructor(aaa: Boolean)

    fun f1(xxx: String): String
}

header fun f2(xxx: Int)

fun testCommon() {
    Foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>zzz<!> = 0)
    val f = Foo(<!NAMED_ARGUMENTS_NOT_ALLOWED!>aaa<!> = true)
    f.f1(<!NAMED_ARGUMENTS_NOT_ALLOWED!>xxx<!> = "")
    f2(xxx = 42)
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl class Foo(val aaa: Boolean) {
    impl constructor(zzz: Int) : this(zzz == 0)

    impl fun f1(xxx: String) = xxx
}

impl fun f2(xxx: Int) {}

fun testPlatform() {
    Foo(zzz = 0)
    val f = Foo(aaa = true)
    f.f1(xxx = "")
    f2(xxx = 42)
}
