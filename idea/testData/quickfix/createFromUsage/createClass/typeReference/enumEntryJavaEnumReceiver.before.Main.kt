// "Create enum constant 'A'" "false"
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
// ACTION: Create annotation 'A'
// ACTION: Create class 'A'
// ACTION: Create enum 'A'
// ACTION: Create interface 'A'
// ACTION: Create type alias 'A'
// ERROR: Unresolved reference: A
internal fun foo(): J.<caret>A = throw Throwable("")