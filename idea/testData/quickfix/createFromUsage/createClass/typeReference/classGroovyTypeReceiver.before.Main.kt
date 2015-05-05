// "Create class 'A'" "false"
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: A
fun foo(): J.<caret>A = throw Throwable("")