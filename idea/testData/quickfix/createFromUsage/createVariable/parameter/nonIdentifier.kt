// "Create parameter '-'" "false"
// ACTION: Create extension function 'minus'
// ACTION: Create member function 'minus'
// ACTION: Replace overloaded operator with function call
// ERROR: Unresolved reference: -
class A

fun bar() {
    val a = A()
    return a <caret>- a
}