// WITH_STDLIB
// DIAGNOSTICS: -ERROR_SUPPRESSION
// FIR_IDENTICAL
// RENDER_ALL_DIAGNOSTICS_FULL_TEXT

// IGNORE_BACKEND: JS_IR, NATIVE
// ^ KT-65680: Class redeclaration leads to BackendException during IR fake override builder

// MODULE: lib
package com.example.klib.serialization.diagnostics

class SeparateModules

// MODULE: main(lib)
// FILE: foo.kt
package com.example.klib.serialization.diagnostics

class SeparateModules

<!CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("PACKAGE_OR_CLASSIFIER_REDECLARATION") class SeparateFiles<!>

// FILE: main.kt
package com.example.klib.serialization.diagnostics

<!CONFLICTING_KLIB_SIGNATURES_ERROR, CONFLICTING_KLIB_SIGNATURES_ERROR!>@Suppress("PACKAGE_OR_CLASSIFIER_REDECLARATION") class SeparateFiles<!>

class ConstructorsClash {
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
    constructor(s: Int)<!>
    <!CONFLICTING_KLIB_SIGNATURES_ERROR!>constructor(s: Int)<!>
}