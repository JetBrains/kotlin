// "Create enum constant 'A'" "false"
// ACTION: Convert to block body
// ACTION: Create annotation 'A'
// ACTION: Create class 'A'
// ACTION: Create enum 'A'
// ACTION: Create interface 'A'
// ERROR: Unresolved reference: A
fun foo(): J.<caret>A = throw Throwable("")