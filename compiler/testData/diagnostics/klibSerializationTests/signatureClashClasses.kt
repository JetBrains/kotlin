// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// DIAGNOSTICS: -ERROR_SUPPRESSION
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT
// RUN_PIPELINE_TILL: BACKEND



// MODULE: lib
package com.example.klib.serialization.diagnostics

class SeparateModules

// MODULE: main(lib)
// FILE: foo.kt
package com.example.klib.serialization.diagnostics

class SeparateModules

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("CLASSIFIER_REDECLARATION") class SeparateFiles<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

<!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("CLASSIFIER_REDECLARATION") class SeparateFiles<!>

class ConstructorsClash {
    @Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>constructor(s: Int)<!>
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>constructor(s: Int)<!>
}