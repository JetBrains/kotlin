// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header fun foo(x: Int, <!HEADER_DECLARATION_WITH_DEFAULT_PARAMETER!>y: String = ""<!>)
