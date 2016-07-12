// NAME: B
// VISIBILITY: private
class A<X, Y>

fun foo() {
    // SIBLING:
    val a: <caret>A<Int, String>
}