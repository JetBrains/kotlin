// "Create type parameter 'X'" "false"
// ACTION: Create class 'X'
// ACTION: Create enum 'X'
// ACTION: Create interface 'X'
// ACTION: Create object 'X'
// ERROR: Unresolved reference: X

class A

fun foo(x: <caret>X.Y) {

}