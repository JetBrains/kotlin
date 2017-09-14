// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect fun foo(x: Int, <!HEADER_DECLARATION_WITH_DEFAULT_PARAMETER!>y: String = ""<!>)
