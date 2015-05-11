// "Create object 'A'" "false"
// ACTION: Create class 'A'
// ACTION: Create interface 'A'
// ACTION: Convert to block body
// ERROR: Unresolved reference: A
package p

fun foo(): <caret>A<Int, String> = throw Throwable("")