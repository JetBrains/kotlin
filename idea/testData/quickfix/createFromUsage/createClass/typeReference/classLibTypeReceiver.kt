// "Create class 'A'" "false"
// ACTION: Convert to block body
// ERROR: Unresolved reference: A
package p

fun foo(): Int.<caret>A = throw Throwable("")