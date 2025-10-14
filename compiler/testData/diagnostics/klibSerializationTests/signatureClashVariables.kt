// WITH_STDLIB
// DIAGNOSTICS: -ERROR_SUPPRESSION
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT


// MODULE: lib
package com.example.klib.serialization.diagnostics

val valueSeparateModules: Int = 0

// MODULE: main(lib)
// FILE: foo.kt
package com.example.klib.serialization.diagnostics

val valueSeparateModules = 0
private val privateValueSeparateFiles = 0

@Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSeparateFiles<!> = 0<!>

@Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!>val myDelegated: Long by lazy { 0L }<!>

@Suppress("REDECLARATION")
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>var Int.extensionValue: Int
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = 0<!>
set(value) = TODO()<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

private val privateValueSeparateFiles = 0

@Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSeparateFiles<!> = 0<!>

@Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSingleFile: Int<!> = 0<!>
@Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSingleFile: String<!> = ""<!>

@Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!>val myDelegated: Int by lazy { 1 }<!>

@Suppress("REDECLARATION")
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>val Int.extensionValue: Int
<!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = 0<!><!>

class Container {
    @Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSameClass<!> = 0<!>
    @Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSameClass<!> = 0<!>

    companion object{
        @Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSameClassCompanion<!> = 0<!>
        @Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!><!CONFLICTING_KLIB_SIGNATURES_ERROR!>val valueSameClassCompanion<!> = 0<!>
    }

    @Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!>val String.valueSameClassExtension: String <!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = ""<!><!>
    @Suppress("REDECLARATION") <!CONFLICTING_KLIB_SIGNATURES_ERROR!>val String.valueSameClassExtension: String <!CONFLICTING_KLIB_SIGNATURES_ERROR!>get() = ""<!><!>
}
