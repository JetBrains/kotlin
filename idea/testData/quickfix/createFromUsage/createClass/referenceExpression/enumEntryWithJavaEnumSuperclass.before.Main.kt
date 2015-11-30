// "Create enum constant 'A'" "false"
// ACTION: Convert to block body
// ACTION: Create member property 'A'
// ERROR: Unresolved reference: A
internal fun foo(): J = J.<caret>A
