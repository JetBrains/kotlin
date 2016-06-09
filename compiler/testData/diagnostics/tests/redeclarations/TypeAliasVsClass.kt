// FILE: file1.kt
class <!REDECLARATION!>SomeClass<!>

typealias <!REDECLARATION!>SomeClass<!> = Any
typealias <!REDECLARATION!>SomeClass<!> = Any
typealias <!REDECLARATION!>SomeClass<!> = Any

class Outer {
    class <!REDECLARATION, CONFLICTING_OVERLOADS!>Nested<!>

    <!CONFLICTING_OVERLOADS!>typealias <!REDECLARATION!>Nested<!> = Any<!>
    <!CONFLICTING_OVERLOADS!>typealias <!REDECLARATION!>Nested<!> = Any<!>
    <!CONFLICTING_OVERLOADS!>typealias <!REDECLARATION!>Nested<!> = Any<!>
}

// FILE: file2.kt
typealias <!REDECLARATION!>SomeClass<!> = Any