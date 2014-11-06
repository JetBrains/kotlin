// "Create enum constant 'A'" "false"
// ACTION: Create object 'A'
// ERROR: Unresolved reference: A
package p

fun foo() = X.<caret>A

class X {

}