// "Create enum constant 'A'" "false"
// ACTION: Create object 'A'
// ACTION: Rename reference
// ACTION: Create extension property 'X.Companion.A'
// ACTION: Create member property 'X.Companion.A'
// ERROR: Unresolved reference: A
package p

fun foo() = X.<caret>A

class X {

}