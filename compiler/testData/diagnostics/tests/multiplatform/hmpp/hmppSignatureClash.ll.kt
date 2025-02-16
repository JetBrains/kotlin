// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND
// MODULE: common
expect class A {
    fun foo(x: String): String
}

// MODULE: intermediate()()(common)
expect class B

actual class <!NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS!>A<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>(x: B) = "a"
}

// MODULE: main()()(intermediate)
actual typealias B = String
