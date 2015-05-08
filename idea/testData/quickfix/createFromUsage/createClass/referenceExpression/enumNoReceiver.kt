// "Create enum 'A'" "false"
// ACTION: Create object 'A'
// ACTION: Create local variable 'A'
// ACTION: Create parameter 'A'
// ACTION: Create property 'A'
// ERROR: Unresolved reference: A
package p

fun foo() = <caret>A