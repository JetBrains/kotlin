// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY
// FILE: file1.kt
<!REDECLARATION!>class SomeClass<!>

<!REDECLARATION!>typealias SomeClass = Any<!>
<!REDECLARATION!>typealias SomeClass = Any<!>
<!REDECLARATION!>typealias SomeClass = Any<!>

class Outer {
    <!REDECLARATION!>class Nested<!>

    <!REDECLARATION!>typealias Nested = Any<!>
    <!REDECLARATION!>typealias Nested = Any<!>
    <!REDECLARATION!>typealias Nested = Any<!>
}

// FILE: file2.kt
<!REDECLARATION!>typealias SomeClass = Any<!>
