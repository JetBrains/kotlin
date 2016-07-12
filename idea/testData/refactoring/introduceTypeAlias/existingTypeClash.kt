// NAME: B
class A<X, Y>

class B

// SIBLING:
fun foo() {
    val a: <caret>A<Int, String>
}