// FILE: a.kt
val <!REDECLARATION, REDECLARATION!>a<!> : Int = 1
<!CONFLICTING_OVERLOADS!>fun f()<!> {
}

// FILE: b.kt
val <!REDECLARATION, REDECLARATION!>a<!> : Int = 1
<!CONFLICTING_OVERLOADS!>fun f()<!> {
}
