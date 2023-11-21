// MODULE: m1-common
// FILE: common.kt

<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect enum class En {
    E1,
    <!EXPECTED_ENUM_ENTRY_WITH_BODY!>E2 {
        <!EXPECTED_DECLARATION_WITH_BODY!>fun foo()<!> = ""
    },<!>
    <!EXPECTED_ENUM_ENTRY_WITH_BODY!>E3 { };<!>
}<!>
