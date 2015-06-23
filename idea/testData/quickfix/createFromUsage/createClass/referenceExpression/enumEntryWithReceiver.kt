// "Create enum constant 'A'" "false"
// ACTION: Create extension property 'A'
// ACTION: Create member property 'A'
// ERROR: Unresolved reference: A
package p

fun foo() = X().<caret>A

class X {

}