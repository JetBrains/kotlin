// NAME: B
// VISIBILITY: private
class A<X, Y>

// SIBLING:
fun foo() {
    val a: <caret>A<Int, String>
}