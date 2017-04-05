// "Create enum constant 'A'" "false"
// ACTION: Create object 'A'
// ACTION: Create local variable 'A'
// ACTION: Create parameter 'A'
// ACTION: Create property 'A'
// ACTION: Import
// ACTION: Rename reference
// ERROR: Unresolved reference: A
package p

fun foo() = <caret>A