// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>header fun foo()<!>

<!CONFLICTING_OVERLOADS, HEADER_DECLARATION_WITH_BODY!>header fun foo()<!> {}

<!HEADER_DECLARATION_WITH_BODY!>header fun bar()<!> {}
