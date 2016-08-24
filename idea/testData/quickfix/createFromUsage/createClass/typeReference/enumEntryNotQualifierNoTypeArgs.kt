// "Create enum constant 'A'" "false"
// ACTION: Create annotation 'A'
// ACTION: Create class 'A'
// ACTION: Create interface 'A'
// ACTION: Create enum 'A'
// ACTION: Create type alias 'A'
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
// ACTION: Create type parameter 'A' in function 'foo'
// ERROR: Unresolved reference: A
package p

internal fun foo(): <caret>A = throw Throwable("")