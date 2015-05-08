// "Create annotation 'A'" "false"
// ACTION: Create class 'A'
// ACTION: Create object 'A'
// ACTION: Create trait 'A'
// ACTION: Create enum 'A'
// ACTION: Convert to block body
// ERROR: Unresolved reference: A
package p

fun foo(): <caret>A.B = throw Throwable("")