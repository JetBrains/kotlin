// FILE: file1.kt
class <!REDECLARATION!>SomeClass<!>

typealias <!REDECLARATION!>SomeClass<!> = Any
typealias <!REDECLARATION!>SomeClass<!> = Any
typealias <!REDECLARATION!>SomeClass<!> = Any

class Outer {
    class <!REDECLARATION!>Nested<!>

    typealias <!REDECLARATION!>Nested<!> = Any
    typealias <!REDECLARATION!>Nested<!> = Any
    typealias <!REDECLARATION!>Nested<!> = Any
}

// FILE: file2.kt
typealias <!REDECLARATION!>SomeClass<!> = Any