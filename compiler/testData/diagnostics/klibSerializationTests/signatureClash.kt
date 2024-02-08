// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// MODULE: lib
package com.example.klib.serialization.diagnostics

fun movedToLib() {}

// MODULE: main(lib)
// FILE: foo.kt
package com.example.klib.serialization.diagnostics

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun foo(): Long = 0L<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR!>var myVal: Long = 0L<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun foo(): String = ""<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun foo(): Int = 0<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
var myVal: Int = 0<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
val myVal: String
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = ""<!><!>

@Deprecated("This function moved to the 'lib' module", level = DeprecationLevel.HIDDEN)
fun movedToLib() {}

fun main() {
    foo()
    movedToLib()

    // Test that the diagnostic is reported for declarations that are referenced before they are declared
    bar()
}

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated("", level = DeprecationLevel.HIDDEN)
fun bar(): Long = 0L<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>fun bar(): Long = 1L<!>
