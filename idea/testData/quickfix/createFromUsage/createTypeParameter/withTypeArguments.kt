// "Create type parameter 'X'" "false"
// ACTION: Create class 'X'
// ACTION: Create interface 'X'
// ERROR: Unresolved reference: X

class A

fun foo(x: <caret>X<Int>) {

}