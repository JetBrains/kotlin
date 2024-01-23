// LANGUAGE: +MultiPlatformProjects
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// MODULE: common
// FILE: common.kt
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): String = ""<!>

expect class A
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun bar(x: A): Int = 2<!>

// MODULE: platform()()(common)
// FILE: platform.kt
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): Int = 0<!>

class B
actual typealias A = B
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun bar(x: B): Int = 3<!>
