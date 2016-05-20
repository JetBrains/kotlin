// "Create class 'A'" "false"
// ACTION: Rename reference
// ACTION: Introduce local variable
// ERROR: Unresolved reference: A
fun foo() = J.<caret>A.B
