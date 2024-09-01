// MODULE: m1-common
// FILE: common.kt

expect <!CONFLICTING_OVERLOADS!>fun foo()<!>

<!EXPECTED_DECLARATION_WITH_BODY!>expect <!CONFLICTING_OVERLOADS!>fun foo()<!><!> {}

<!EXPECTED_DECLARATION_WITH_BODY!>expect fun bar()<!> {}
