// "Create type parameter 'X'" "false"
// ACTION: Create class 'X'
// ACTION: Create interface 'X'
// ACTION: Create type alias 'X'
// ERROR: Unresolved reference: X

class A

fun foo(x: <caret>X<Int>) {

}