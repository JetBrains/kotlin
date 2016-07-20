// "Create type alias 'A'" "false"
// ACTION: Convert to block body
// ACTION: Create class 'A'
// ACTION: Create enum 'A'
// ACTION: Create interface 'A'
// ACTION: Create object 'A'
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: A
package p

fun foo(): <caret>A.B = throw Throwable("")