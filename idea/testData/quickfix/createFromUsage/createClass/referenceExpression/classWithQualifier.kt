// "Create class 'A'" "false"
// ACTION: Create object 'A'
// ACTION: Rename reference
// ERROR: Unresolved reference: A
package p

fun foo() = X.<caret>A

class X {

}