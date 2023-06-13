// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun foo()<!>

<!CONFLICTING_OVERLOADS, EXPECTED_DECLARATION_WITH_BODY!>expect fun foo()<!> {}

<!EXPECTED_DECLARATION_WITH_BODY!>expect fun bar()<!> {}
