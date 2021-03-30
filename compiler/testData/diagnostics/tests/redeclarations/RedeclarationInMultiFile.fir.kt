// FILE: a.kt
<!REDECLARATION!>val a : Int = 1<!>
<!CONFLICTING_OVERLOADS!>fun f()<!> {
}

// FILE: b.kt
<!REDECLARATION!>val a : Int = 1<!>
<!CONFLICTING_OVERLOADS!>fun f()<!> {
}
