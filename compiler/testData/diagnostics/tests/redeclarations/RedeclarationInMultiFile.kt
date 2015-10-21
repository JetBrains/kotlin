// FILE: a.kt
val <!REDECLARATION!>a<!> : Int = 1
<!CONFLICTING_OVERLOADS!>fun f()<!> {
}

// FILE: b.kt
val <!REDECLARATION!>a<!> : Int = 1
<!CONFLICTING_OVERLOADS!>fun f()<!> {
}
