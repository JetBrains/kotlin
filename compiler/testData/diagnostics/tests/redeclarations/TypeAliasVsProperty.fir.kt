// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// FILE: file1.kt
typealias <!REDECLARATION!>Test<!> = String

val <!REDECLARATION!>Test<!> = 42

class Outer {
    typealias <!REDECLARATION!>Test<!> = String

    val <!REDECLARATION!>Test<!> = 42
}

typealias Test2 = String

// FILE: file2.kt
val Test2 = 42
