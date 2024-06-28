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
actual object <!ACTUAL_WITHOUT_EXPECT!>E02<!>
actual object <!ACTUAL_WITHOUT_EXPECT!>E03<!>

actual object <!ACTUAL_WITHOUT_EXPECT!>E04<!>

actual object <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E05<!>
actual object <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>E06<!>

actual object <!ACTUAL_WITHOUT_EXPECT!>I01<!>

actual object M01 {
    actual fun foo() {}
}

actual object <!ACTUAL_WITHOUT_EXPECT!>ENUM01<!>

actual object <!ACTUAL_WITHOUT_EXPECT!>ANNO01<!>
