// "Create type parameter 'X'" "false"
// ACTION: Create class 'X'
// ACTION: Create enum 'X'
// ACTION: Create interface 'X'
// ACTION: Create object 'X'
// ACTION: Enable a trailing comma by default in the formatter
// ERROR: Unresolved reference: X

class A

fun foo(x: <caret>X.Y) {

}