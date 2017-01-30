// "Create class 'A'" "false"
// ACTION: Create object 'A'
// ACTION: Create local variable 'A'
// ACTION: Create parameter 'A'
// ACTION: Create property 'A'
// ACTION: Rename reference
// ACTION: Import
// ERROR: Unresolved reference: A
package p

fun foo() = <caret>A