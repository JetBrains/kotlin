// "Create type parameter 'X'" "false"
// ACTION: Create annotation 'X'
// ACTION: Create class 'X'
// ACTION: Create enum 'X'
// ACTION: Create interface 'X'
// ACTION: Enable a trailing comma by default in the formatter
// ERROR: Unresolved reference: X

class A

fun foo(x: A.<caret>X) {

}