// "Create parameter '-'" "false"
// ACTION: Create extension function 'A.minus'
// ACTION: Create member function 'A.minus'
// ACTION: Replace overloaded operator with function call
// ERROR: Unresolved reference: -
class A

fun bar() {
    val a = A()
    return a <caret>- a
}