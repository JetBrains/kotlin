// MODULE: m1-common
// FILE: common.kt

expect class E01
expect class E02()
expect open class E03

expect class E04 {
    constructor()
}

expect class E05(e: E01)
expect class E06 {
    constructor(e: E02)
}

expect interface I01

expect class M01 {
    fun foo()
}

expect enum class ENUM01

expect annotation class ANNO01

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual object E01
actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E02
actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E03

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E04

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E05
actual <!ACTUAL_WITHOUT_EXPECT!>object<!> E06

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> I01

actual object M01 {
    actual fun foo() {}
}

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> ENUM01

actual <!ACTUAL_WITHOUT_EXPECT!>object<!> ANNO01
