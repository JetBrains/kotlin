// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: BACKEND

// MODULE: common_1
// FILE: common1.kt
expect class A() {
    fun foo(): String
}

// MODULE: common_2
// FILE: common2.kt
expect class A() {
    fun bar(): String
}

// MODULE: main()()(common_1, common_2)
// FILE: test.kt
<!AMBIGUOUS_EXPECTS!>actual<!> class A <!ACTUAL_WITHOUT_EXPECT!>actual constructor()<!> {
    actual fun <!ACTUAL_WITHOUT_EXPECT!>foo<!>(): String = "O"
    actual fun <!ACTUAL_WITHOUT_EXPECT!>bar<!>(): String = "K"
}
