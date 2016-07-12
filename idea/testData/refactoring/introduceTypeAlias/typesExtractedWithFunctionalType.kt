// NAME: F
class A<X, Y>

// SIBLING:
fun foo() {
    class B<X>

    val a: A<<caret>(B<Int>) -> B<String>, String>
}