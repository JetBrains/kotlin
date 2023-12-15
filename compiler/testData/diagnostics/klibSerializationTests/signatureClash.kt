// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// FILE: foo.kt
package com.example.klib.serialization.diagnostics

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun foo(): Long = 0L<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun foo(): String = ""<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): Int = 0<!>

fun main() {
    foo()
}
