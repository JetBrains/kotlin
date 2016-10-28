// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

platform fun foo(x: Int, <!PLATFORM_DECLARATION_WITH_DEFAULT_PARAMETER!>y: String = ""<!>)
