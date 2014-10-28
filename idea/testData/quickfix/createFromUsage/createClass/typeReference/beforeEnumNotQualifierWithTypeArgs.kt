// "Create enum 'A'" "false"
// ACTION: Create class 'A'
// ACTION: Create trait 'A'
// ACTION: Convert to block body
// ERROR: Unresolved reference: A
package p

fun foo(): <caret>A<Int, String> = throw Throwable("")