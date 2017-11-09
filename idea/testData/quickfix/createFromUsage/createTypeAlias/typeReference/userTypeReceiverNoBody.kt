// "Create type alias 'A'" "false"
// ACTION: Convert to block body
// ACTION: Create annotation 'A'
// ACTION: Create class 'A'
// ACTION: Create enum 'A'
// ACTION: Create interface 'A'
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: A
package p

class T

fun foo(): T.<caret>A = throw Throwable("")