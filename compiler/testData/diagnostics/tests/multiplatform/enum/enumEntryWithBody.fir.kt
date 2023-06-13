// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT!>expect enum class En {
    E1,
    E2 {
        <!EXPECTED_DECLARATION_WITH_BODY!>fun foo()<!> = ""
    },
    E3 { };
}<!>
