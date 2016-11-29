// "Create type parameter 'X'" "false"
// ACTION: Create annotation 'X'
// ACTION: Create class 'X'
// ACTION: Create enum 'X'
// ACTION: Create interface 'X'
// ERROR: Unresolved reference: X

class A

fun foo(x: A.<caret>X) {

}