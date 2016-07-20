// "Create annotation 'A'" "false"
// ACTION: Create class 'A'
// ACTION: Create interface 'A'
// ACTION: Create type alias 'A'
// ACTION: Convert to block body
// ACTION: Remove explicit type specification
// ERROR: Unresolved reference: A
package p

internal fun foo(): <caret>A<Int, String> = throw Throwable("")