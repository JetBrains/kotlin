// LANGUAGE: +MultiPlatformProjects
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// MODULE: common
// FILE: common.kt
<!CONFLICTING_OVERLOADS!>fun foo(): String<!> = ""

expect class A
<!CONFLICTING_OVERLOADS!>fun bar(x: A): Int<!> = 2

@Suppress("REDECLARATION")
val param = 0

// MODULE: platform()()(common)
// FILE: platform.kt
fun foo(): Int = 0

class B
actual typealias A = B
fun bar(x: B): Int = 3

@Suppress("REDECLARATION")
val param = 0
