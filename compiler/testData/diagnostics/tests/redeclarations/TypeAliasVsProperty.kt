// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// FILE: file1.kt
typealias <!REDECLARATION!>Test<!> = String

val <!REDECLARATION!>Test<!> = 42

class Outer {
    typealias <!REDECLARATION!>Test<!> = String

    val <!REDECLARATION!>Test<!> = 42
}

typealias <!REDECLARATION!>Test2<!> = String

// FILE: file2.kt
val <!REDECLARATION!>Test2<!> = 42
