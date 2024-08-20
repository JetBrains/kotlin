// WITH_STDLIB
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// MODULE: lib
package com.example.klib.serialization.diagnostics

val valueSeparateModules: Int = 0

// MODULE: main(lib)
// FILE: foo.kt
package com.example.klib.serialization.diagnostics

val valueSeparateModules = 0
private val privateValueSeparateFiles = 0

<!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSeparateFiles<!> = 0<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val myDelegated: Long by <!CONFLICTING_KLIB_SIGNATURES_ERROR!>lazy { 0L }<!><!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION")
var Int.extensionValue: Int
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = 0<!>
set(value) = TODO()<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

private val privateValueSeparateFiles = 0

<!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSeparateFiles<!> = 0<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSingleFile: Int<!> = 0<!>
<!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSingleFile: String<!> = ""<!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val myDelegated: Int by <!CONFLICTING_KLIB_SIGNATURES_ERROR!>lazy { 1 }<!><!>

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION")
val Int.extensionValue: Int
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = 0<!><!>

class Container {
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSameClass<!> = 0<!>
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSameClass<!> = 0<!>

    companion object{
        <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSameClassCompanion<!> = 0<!>
        <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val valueSameClassCompanion<!> = 0<!>
    }

    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val String.valueSameClassExtension: String <!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = ""<!><!>
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("REDECLARATION") val String.valueSameClassExtension: String <!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = ""<!><!>
}
