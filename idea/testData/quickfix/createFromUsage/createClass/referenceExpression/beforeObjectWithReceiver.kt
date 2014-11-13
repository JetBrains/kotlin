// "Create object 'A'" "false"
// ACTION: Create property 'A'
// ERROR: Unresolved reference: A
package p

fun foo() = X().<caret>A

class X {

}