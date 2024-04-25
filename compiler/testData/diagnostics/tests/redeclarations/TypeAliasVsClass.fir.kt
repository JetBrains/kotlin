// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// FILE: file1.kt
class <!CLASSIFIER_REDECLARATION!>SomeClass<!>

typealias <!CLASSIFIER_REDECLARATION!>SomeClass<!> = Any
typealias <!CLASSIFIER_REDECLARATION!>SomeClass<!> = Any
typealias <!CLASSIFIER_REDECLARATION!>SomeClass<!> = Any

class Outer {
    class <!REDECLARATION!>Nested<!>

    typealias <!REDECLARATION!>Nested<!> = Any
    typealias <!REDECLARATION!>Nested<!> = Any
    typealias <!REDECLARATION!>Nested<!> = Any
}

// FILE: file2.kt
typealias <!CLASSIFIER_REDECLARATION!>SomeClass<!> = Any