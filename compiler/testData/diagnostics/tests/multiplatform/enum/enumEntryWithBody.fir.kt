// MODULE: m1-common
// FILE: common.kt

expect enum class En {
    E1,
    E2 {
        <!EXPECTED_DECLARATION_WITH_BODY!>fun foo()<!> = ""
    },
    E3 { };
}
