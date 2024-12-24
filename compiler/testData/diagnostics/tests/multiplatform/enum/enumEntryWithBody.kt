// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FRONTEND
// MODULE: m1-common
// FILE: common.kt

expect enum class <!NO_ACTUAL_FOR_EXPECT!>En<!> {
    E1,
    <!EXPECTED_ENUM_ENTRY_WITH_BODY!>E2 {
        <!EXPECTED_DECLARATION_WITH_BODY!>fun foo()<!> = ""
    },<!>
    <!EXPECTED_ENUM_ENTRY_WITH_BODY!>E3 { };<!>
}
