// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>platform fun foo()<!>

<!CONFLICTING_OVERLOADS, PLATFORM_DECLARATION_WITH_BODY!>platform fun foo()<!> {}

<!PLATFORM_DECLARATION_WITH_BODY!>platform fun bar()<!> {}
