// MODULE: m1-common
// FILE: common.kt
annotation class Ann

expect class CompatibleOverrides {
    fun foo()

    @Ann
    fun foo(withArg: Any)
}

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
actual class CompatibleOverrides {
    actual fun foo() {}

    <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual fun <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>foo<!>(withArg: Any) {}<!>
}
