// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>expect fun foo()<!>

<!CONFLICTING_OVERLOADS!>expect fun foo()<!> {}

expect fun bar() {}
