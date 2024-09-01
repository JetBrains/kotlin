// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-24047

// MODULE: m1-common
// FILE: common.kt

expect class Foo {
    fun f(): Foo?
    fun f(x: Int): Foo?
    fun f(x: Double, y: Int): Foo?
}

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>Foo<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>f<!>() = <!TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM_ERROR!><!DEBUG_INFO_MISSING_UNRESOLVED!>f<!>(<!DEBUG_INFO_MISSING_UNRESOLVED!>-<!>1)<!>
    actual fun f(x: Int): Foo? = null
    actual fun f(x: Double, y: Int): Foo? = null
}
